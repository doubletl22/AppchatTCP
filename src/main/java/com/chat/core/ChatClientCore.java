package com.chat.core; // Đã đổi package

import com.chat.model.Message; // Đã đổi import
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChatClientCore {
    private volatile boolean connected = false;
    private volatile boolean authenticated = false;
    private String userName = "User";
    private final List<String> currentUsers = new ArrayList<>();

    private Socket socket;
    private BufferedReader reader;
    private BufferedOutputStream out;
    private Thread recvThread;
    private final Gson gson = new Gson();
    private ClientStatusListener listener;

    public ChatClientCore(ClientStatusListener listener) {
        this.listener = listener;
    }

    // Setter mới để Controller có thể set chính nó làm Listener
    public void setListener(ClientStatusListener listener) {
        this.listener = listener;
    }

    public boolean isConnected() { return connected; }
    public boolean isAuthenticated() { return authenticated; }

    public void connectAndAuth(String host, int port, String username, String password, String action) throws IOException {
        if (connected) return;

        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new BufferedOutputStream(socket.getOutputStream());

            Message authMsg = "login".equals(action)
                    ? Message.login(username, password)
                    : Message.register(username, password);

            String json = gson.toJson(authMsg) + "\n";
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            disconnect(e.getMessage());
            throw e;
        }

        connected = true;

        recvThread = new Thread(this::recvLoop, "recv-loop");
        recvThread.setDaemon(true);
        recvThread.start();
    }

    public void disconnect(String reason) {
        if (!connected) return;
        connected = false;
        authenticated = false;
        userName = "User";
        currentUsers.clear();
        try { socket.close(); } catch (Exception ignored) {}
        socket = null;

        if (listener != null) listener.onDisconnect(reason);
    }

    private void recvLoop() {
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                try {
                    Message m = gson.fromJson(line, Message.class);
                    if (m == null) continue;

                    if ("auth_success".equals(m.type)) {
                        authenticated = true;
                        userName = m.name;
                        currentUsers.add(userName);
                        if (listener != null) listener.onConnectSuccess(userName);
                    } else if ("auth_failure".equals(m.type)) {
                        if (listener != null) listener.onAuthFailure(m.text);
                        disconnect(null);
                        return;
                    } else if ("user_list".equals(m.type) && m.users != null) {
                        currentUsers.clear();
                        currentUsers.add(userName);
                        currentUsers.addAll(m.users);

                        List<String> listToSend = currentUsers.stream()
                                .sorted()
                                .filter(n -> !n.equals(userName))
                                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                        if (listener != null) listener.onUserListUpdate(listToSend, userName);
                    } else if (authenticated) {
                        if ("system".equals(m.type)) {
                            if (listener != null) listener.onSystemMessage(m.text);

                            String name = null;
                            if (m.text.endsWith(" joined the chat.")) {
                                name = m.text.substring(0, m.text.indexOf(" joined the chat."));
                                if (!currentUsers.contains(name)) currentUsers.add(name);
                            } else if (m.text.endsWith(" left the chat.") || m.text.endsWith(" was kicked by server.")) {
                                name = m.text.substring(0, m.text.indexOf(" left the chat."));
                                if (name.equals(m.text)) name = m.text.substring(0, m.text.indexOf(" was kicked by server."));
                                currentUsers.remove(name);
                            }

                            if (name != null) {
                                List<String> listToSend = currentUsers.stream()
                                        .sorted()
                                        .filter(n -> !n.equals(userName))
                                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                                if (listener != null) listener.onUserListUpdate(listToSend, userName);
                            }
                        } else {
                            if (listener != null) listener.onMessageReceived(m);
                        }
                    }
                } catch (JsonSyntaxException ignore) {}
            }
        } catch (IOException ignore) {
        } finally {
            if (connected) {
                if (listener != null) listener.onSystemMessage("Connection lost.");
                disconnect("Connection lost.");
            }
        }
    }

    public void sendMessage(String text, String recipient) throws IOException {
        if (!connected || !authenticated || out == null) throw new IOException("Not connected or authenticated.");

        Message msgToSend;
        if ("Public Chat".equals(recipient)) {
            msgToSend = Message.chat(text); // Server sẽ tự điền name
        } else {
            msgToSend = Message.direct(recipient, text); // Server sẽ tự điền name
        }

        String json = gson.toJson(msgToSend) + "\n";
        out.write(json.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /**
     * NEW: Gửi tin nhắn GIF lên Server.
     */
    public void sendGif(String gifKeyword, String recipient) throws IOException {
        if (!connected || !authenticated || out == null) throw new IOException("Not connected or authenticated.");

        Message msgToSend = Message.gif(gifKeyword, recipient);

        String json = gson.toJson(msgToSend) + "\n";
        out.write(json.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /**
     * [MỚI] Gửi tin nhắn Voice (Base64 Audio) lên Server.
     */
    public void sendVoice(String base64Data, String recipient) throws IOException {
        if (!connected || !authenticated || out == null) throw new IOException("Not connected or authenticated.");

        // Sử dụng Message.voice factory method mới (cần thêm vào Message.java)
        Message msgToSend = Message.voice(base64Data, recipient);

        String json = gson.toJson(msgToSend) + "\n";
        out.write(json.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    // Phương thức mới: Yêu cầu lịch sử DM
    public void requestDirectHistory(String targetName) throws IOException {
        if (!connected || !authenticated || out == null) throw new IOException("Not connected or authenticated.");

        Message msgToSend = Message.getDirectHistory(targetName);

        String json = gson.toJson(msgToSend) + "\n";
        out.write(json.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}