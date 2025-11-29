package com.chat.core;

import com.chat.model.Message;
import com.chat.service.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ChatServerCore extends Thread {
    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private final List<ClientConn> clients = new CopyOnWriteArrayList<>();
    private final Gson gson = new Gson();
    private final DatabaseManager dbManager;
    private final ServerLogListener logListener;

    public ChatServerCore(int port, DatabaseManager dbManager, ServerLogListener logListener) {
        this.port = port;
        this.dbManager = dbManager;
        this.logListener = logListener;
    }

    public boolean isRunning() { return running; }

    public void startServer() throws IOException {
        try {
            // Lấy SSL Factory mặc định. Yêu cầu cấu hình Keystore qua System Properties.
            SSLServerSocketFactory sslFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            serverSocket = sslFactory.createServerSocket(port); // Tạo SSL Server Socket
        } catch (Exception e) {
            logListener.log("Lỗi SSL Server: " + e.getMessage());
            throw new IOException("Không thể khởi tạo SSL Server. Hãy kiểm tra cấu hình Keystore.", e);
        }
        // --- Kết thúc phần triển khai SSL/TLS ---

        running = true;
        logListener.log("Server started securely (SSL/TLS) on port " + port + ". Ready for authentication."); // Cập nhật log
        this.start();
    }

    public void stopServer() {
        if (!running) return;
        running = false;
        try {
            for (ClientConn c : new ArrayList<>(clients)) { c.close(); }
            clients.clear();
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {}
        logListener.refreshClientList(Collections.emptyList());
        logListener.log("Server stopped.");
    }

    public void kickClient(String nameToKick) {
        for (ClientConn c : clients) {
            if (nameToKick.equals(c.name)) {
                c.close();
                logListener.log("Kicked: " + nameToKick);
                broadcast(Message.system(nameToKick + " was kicked by server."));
                break;
            }
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                Socket sock = serverSocket.accept();
                ClientConn conn = new ClientConn(sock);
                conn.start();
            } catch (IOException ex) {
                if (running) logListener.log("Accept error: " + ex.getMessage());
            }
        }
    }

    // --- XỬ LÝ BROADCAST (Gửi cho tất cả) ---
    public void broadcast(Message m) {
        String logText = null;

        if ("chat".equals(m.type)) {
            dbManager.storeMessage(m.name, m.text);
            logText = "[CHAT] " + m.name + ": " + m.text;

        } else if ("gif".equals(m.type)) {
            dbManager.storeMessage(m.name, "[GIF]: " + m.text);
            logText = "[GIF] " + m.name + ": " + m.text;

        } else if ("voice".equals(m.type)) {
            // Xử lý tin nhắn thoại công khai
            dbManager.storeMessage(m.name, "[Tin nhắn thoại]");
            logText = "[VOICE] " + m.name + " sent a voice message.";

        } else if ("image".equals(m.type)) {
            // Xử lý tin nhắn ảnh công khai
            dbManager.storeMessage(m.name, "[Hình ảnh]");
            logText = "[IMAGE] " + m.name + " sent an image.";

        } else if ("sticker".equals(m.type)) {
            // Xử lý tin nhắn Sticker công khai
            // Lưu đường dẫn sticker vào DB với tiền tố [STICKER]:
            dbManager.storeMessage(m.name, "[STICKER]:" + m.text);
            logText = "[STICKER] " + m.name + ": " + m.text;

        } else if ("system".equals(m.type)) {
            logText = m.text;
        }

        // Gửi tin nhắn đến tất cả clients đang kết nối
        for (ClientConn c : new ArrayList<>(clients)) {
            try {
                sendToClient(c, m);
            } catch (Exception ex) {
                c.close();
            }
        }

        if (logText != null) {
            logListener.log(logText);
        }
    }

    // --- XỬ LÝ TIN NHẮN RIÊNG (Private Message) ---
    private void sendPrivateMessage(ClientConn sender, Message m) {
        String targetName = m.targetName;
        ClientConn target = clients.stream()
                .filter(c -> targetName.equals(c.name))
                .findFirst().orElse(null);

        // Chuẩn bị dữ liệu để lưu DB và Log
        String storedMessage = m.text;
        String logPrefix = "[DM] ";

        // Xác định loại tin nhắn để gửi đi đúng format
        String msgTypeToTarget = "dm";
        String msgTypeToSender = "dm";

        if ("dm_gif".equals(m.type)) {
            storedMessage = "[GIF]: " + m.text;
            logPrefix = "[DM GIF] ";
            msgTypeToTarget = "dm_gif";
            msgTypeToSender = "dm_gif";
        } else if ("dm_voice".equals(m.type)) {
            storedMessage = "[Tin nhắn thoại]";
            logPrefix = "[DM VOICE] ";
            msgTypeToTarget = "dm_voice";
            msgTypeToSender = "dm_voice";
        } else if ("dm_image".equals(m.type)) {
            storedMessage = "[Hình ảnh]";
            logPrefix = "[DM IMAGE] ";
            msgTypeToTarget = "dm_image";
            msgTypeToSender = "dm_image";
        } else if ("dm_sticker".equals(m.type)) {
            storedMessage = "[STICKER]:" + m.text;
            logPrefix = "[DM STICKER] ";
            msgTypeToTarget = "dm_sticker";
            msgTypeToSender = "dm_sticker";
        }

        // Lưu vào DB
        dbManager.storeDirectMessage(sender.name, targetName, storedMessage);

        if (target == null) {
            sendToClient(sender, Message.system("Người dùng " + targetName + " không online."));
            return;
        }

        // 1. Gửi cho người nhận (Target)
        Message msgToTarget = new Message();
        msgToTarget.type = msgTypeToTarget;
        msgToTarget.name = sender.name;
        msgToTarget.targetName = targetName;
        msgToTarget.text = m.text;
        msgToTarget.data = m.data; // Copy dữ liệu âm thanh/hình ảnh

        // 2. Gửi xác nhận cho người gửi (Sender)
        Message msgToSender = new Message();
        msgToSender.type = msgTypeToSender;
        msgToSender.name = "[TO " + targetName + "]";
        msgToSender.targetName = targetName;
        msgToSender.text = m.text;
        // Không gửi lại data cho người gửi để tiết kiệm băng thông

        sendToClient(target, msgToTarget);
        sendToClient(sender, msgToSender);

        logListener.log(logPrefix + sender.name + " -> " + targetName);
    }

    private void sendToClient(ClientConn client, Message m) {
        String json = gson.toJson(m) + "\n";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        try {
            client.out.write(bytes);
            client.out.flush();
        } catch (IOException ex) {
            client.close();
        }
    }

    private void refreshClientList() {
        List<String> clientNames = clients.stream().map(c -> c.name).collect(Collectors.toList());
        logListener.refreshClientList(clientNames);
    }

    // --- LỚP QUẢN LÝ KẾT NỐI CLIENT (Nested Class) ---
    private class ClientConn extends Thread {
        final Socket socket;
        final BufferedReader reader;
        final BufferedOutputStream out;
        String name = null;
        volatile boolean open = true;
        volatile boolean authenticated = false;

        ClientConn(Socket socket) throws IOException {
            super("client-" + socket.getRemoteSocketAddress());
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new BufferedOutputStream(socket.getOutputStream());
        }

        @Override
        public void run() {
            try {
                // --- Vòng lặp xác thực (Login/Register) ---
                while (open && !authenticated) {
                    String line = reader.readLine();
                    if (line == null) { close(); return; }

                    Message m;
                    try {
                        m = gson.fromJson(line, Message.class);
                    } catch (JsonSyntaxException ignore) {
                        continue;
                    }

                    if (m == null || m.type == null) continue;

                    if ("register".equals(m.type)) {
                        if (dbManager.registerUser(m.username, m.password)) {
                            sendToClient(this, Message.authFailure("Đăng ký thành công. Hãy đăng nhập."));
                            logListener.log("User registered: " + m.username);
                        } else {
                            sendToClient(this, Message.authFailure("Tên đăng nhập đã tồn tại."));
                        }
                    } else if ("login".equals(m.type)) {
                        if (dbManager.authenticateUser(m.username, m.password)) {
                            name = m.username;
                            authenticated = true;
                            sendToClient(this, Message.authSuccess(name));
                            logListener.log("Authenticated: " + name + " @ " + socket.getRemoteSocketAddress());

                            // Tải lịch sử chat cũ
                            List<Message> history = dbManager.getChatHistory(50);
                            for (Message chatMsg : history) { sendToClient(this, chatMsg); }
                            sendToClient(this, Message.system("Chat history loaded."));

                            clients.add(this);
                            refreshClientList();

                            // Gửi danh sách user online
                            List<String> currentNames = clients.stream()
                                    .map(c -> c.name)
                                    .filter(n -> !n.equals(name))
                                    .collect(Collectors.toList());
                            sendToClient(this, Message.userlist(currentNames));

                            broadcast(Message.system(name + " joined the chat."));
                            break;
                        } else {
                            sendToClient(this, Message.authFailure("Sai tên đăng nhập hoặc mật khẩu."));
                        }
                    }
                }

                // --- Vòng lặp chat chính ---
                while (open && authenticated) {
                    String l = reader.readLine();
                    if (l == null) break;
                    try {
                        Message m = gson.fromJson(l, Message.class);
                        if (m != null) {
                            // 1. Xử lý Chat công khai (Text, GIF, Voice, Image, Sticker)
                            if ("chat".equals(m.type) || "gif".equals(m.type) || "voice".equals(m.type) || "image".equals(m.type) || "sticker".equals(m.type)) {
                                Message outMsg = new Message();
                                outMsg.type = m.type;
                                outMsg.name = name;
                                outMsg.text = m.text;
                                outMsg.data = m.data; // Copy dữ liệu voice/image
                                broadcast(outMsg);

                                // 2. Xử lý Chat riêng tư (DM Text, DM GIF, DM Voice, DM Image, DM Sticker)
                            } else if (("dm".equals(m.type) || "dm_gif".equals(m.type) || "dm_voice".equals(m.type) || "dm_image".equals(m.type) || "dm_sticker".equals(m.type))
                                    && m.targetName != null) {
                                m.name = name;
                                sendPrivateMessage(this, m);

                                // 3. Xử lý yêu cầu lịch sử DM
                            } else if ("get_dm_history".equals(m.type) && m.targetName != null) {
                                handleDirectHistoryRequest(this, m.targetName);

                                // 4. [MỚI] Xử lý yêu cầu tải lại lịch sử Chat Chung
                            } else if ("get_chat_history".equals(m.type)) {
                                List<Message> history = dbManager.getChatHistory(50);
                                sendToClient(this, Message.system("--- Public Chat History Reloaded ---"));
                                for (Message chatMsg : history) {
                                    sendToClient(this, chatMsg);
                                }
                            }
                        }
                    } catch (JsonSyntaxException ignore) {}
                }
            } catch (IOException e) {
                // Connection error
            } finally {
                close();
            }
        }

        void close() {
            if (!open) return;
            open = false;
            try { socket.close(); } catch (Exception ignored) {}
            clients.remove(this);
            if (name != null) {
                refreshClientList();
                broadcast(Message.system(name + " left the chat."));
                logListener.log("Disconnected: " + name);
            }
        }
    }

    private void handleDirectHistoryRequest(ClientConn client, String targetName) {
        try {
            List<Message> history = dbManager.getDirectMessageHistory(client.name, targetName, 50);
            sendToClient(client, Message.system("--- Lịch sử tin nhắn với " + targetName + " ---"));
            for (Message dmMsg : history) {
                sendToClient(client, dmMsg);
            }
        } catch (Exception ex) {
            logListener.log("Error loading DM history: " + ex.getMessage());
        }
    }
}