package com.chat.core;

import com.chat.model.Message;
import com.chat.service.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

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
        if (running) return;
        serverSocket = new ServerSocket(port);
        running = true;
        logListener.log("Server started on 0.0.0.0:" + port + ". Ready for authentication.");
        this.start(); // Start the accept loop thread
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

    public void broadcast(Message m) {
        if ("chat".equals(m.type)) {
            dbManager.storeMessage(m.name, m.text);
        }

        for (ClientConn c : new ArrayList<>(clients)) {
            try {
                sendToClient(c, m);
            } catch (Exception ex) {
                c.close();
            }
        }
        if ("chat".equals(m.type)) {
            logListener.log(m.name + ": " + m.text);
        } else if ("system".equals(m.type)) {
            logListener.log(m.text);
        }
    }

    private void sendPrivateMessage(ClientConn sender, Message m) {
        String targetName = m.targetName;
        ClientConn target = clients.stream()
                .filter(c -> targetName.equals(c.name))
                .findFirst().orElse(null);

        // Store DM message
        dbManager.storeDirectMessage(sender.name, targetName, m.text);

        if (target == null) {
            sendToClient(sender, Message.system("Người dùng " + targetName + " không kết nối hoặc không tồn tại"));
            return;
        }

        // 1. Message for target
        Message msgToTarget = new Message();
        msgToTarget.type = "dm";
        msgToTarget.name = sender.name;
        msgToTarget.targetName = targetName;
        msgToTarget.text = m.text;

        // 2. Confirmation message for sender
        Message msgToSender = new Message();
        msgToSender.type = "dm";
        msgToSender.name = "[TO " + targetName + "]";
        msgToSender.targetName = targetName;
        msgToSender.text = m.text;

        sendToClient(target, msgToTarget);
        sendToClient(sender, msgToSender);

        logListener.log("[DM] " + sender.name + " -> " + targetName + ": " + m.text);
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
                // Authentication Loop
                while (open && !authenticated) {
                    String line = reader.readLine();
                    if (line == null) { close(); return; }

                    Message m = null;
                    try {
                        m = gson.fromJson(line, Message.class);
                    } catch (JsonSyntaxException ignore) {
                        sendToClient(this, Message.authFailure("Invalid JSON format."));
                        continue;
                    }

                    if (m == null || m.username == null || m.password == null) {
                        sendToClient(this, Message.authFailure("Missing username or password."));
                        continue;
                    }

                    if ("register".equals(m.type)) {
                        if (dbManager.registerUser(m.username, m.password)) {
                            sendToClient(this, Message.authFailure("Registration successful. Please login now."));
                            logListener.log("User registered: " + m.username);
                        } else {
                            sendToClient(this, Message.authFailure("Username already exists or registration failed."));
                        }
                    } else if ("login".equals(m.type)) {
                        if (dbManager.authenticateUser(m.username, m.password)) {
                            name = m.username;
                            authenticated = true;
                            sendToClient(this, Message.authSuccess(name));
                            logListener.log("Client authenticated: " + name + " @ " + socket.getRemoteSocketAddress());

                            List<Message> history = dbManager.getChatHistory(100);
                            for (Message chatMsg : history) { sendToClient(this, chatMsg); }
                            sendToClient(this, Message.system("Chat history loaded."));

                            clients.add(this);
                            refreshClientList();

                            List<String> currentNames = clients.stream()
                                    .map(c -> c.name)
                                    .filter(n -> !n.equals(name))
                                    .collect(Collectors.toList());
                            sendToClient(this, Message.userlist(currentNames));

                            broadcast(Message.system(name + " joined the chat."));
                            break;
                        } else {
                            sendToClient(this, Message.authFailure("Login failed: Invalid username or password."));
                            logListener.log("Login attempt failed for: " + m.username);
                        }
                    } else {
                        sendToClient(this, Message.authFailure("Must login or register first."));
                    }
                }

                // Main chat loop
                while (open && authenticated) {
                    String l = reader.readLine();
                    if (l == null) break;
                    try {
                        Message m = gson.fromJson(l, Message.class);
                        if (m != null) {
                            if ("chat".equals(m.type)) {
                                Message outMsg = new Message();
                                outMsg.type = "chat";
                                outMsg.name = name;
                                outMsg.text = m.text;
                                broadcast(outMsg);
                            } else if ("dm".equals(m.type) && m.targetName != null) {
                                m.name = name;
                                sendPrivateMessage(this, m);
                            } else if ("get_dm_history".equals(m.type) && m.targetName != null) {
                                handleDirectHistoryRequest(this, m.targetName);
                            }
                        }
                    } catch (JsonSyntaxException ignore) {}
                }
            } catch (IOException e) {
                // Connection closed or error
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
                logListener.log("Client disconnected: " + name);
            }
        }
    }

    /**
     * Xử lý yêu cầu lấy lịch sử DM từ client.
     */
    private void handleDirectHistoryRequest(ClientConn client, String targetName) {
        try {
            List<Message> history = dbManager.getDirectMessageHistory(client.name, targetName, 50);
            sendToClient(client, Message.system("--- Lịch sử tin nhắn với " + targetName + " đã tải ---"));
            for (Message dmMsg : history) {
                dmMsg.type = "dm_history";
                sendToClient(client, dmMsg);
            }
        } catch (Exception ex) {
            logListener.log("Error loading DM history for " + client.name + ": " + ex.getMessage());
            sendToClient(client, Message.system("Không thể tải lịch sử tin nhắn."));
        }
    }
}