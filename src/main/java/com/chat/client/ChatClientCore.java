package com.chat.client;

import com.chat.Message;
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
    private final ClientStatusListener listener; // Presentation dependency

    public ChatClientCore(ClientStatusListener listener) {
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
                    ? com.chat.Message.login(username, password)
                    : com.chat.Message.register(username, password);

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

        listener.onDisconnect(reason);
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
                        listener.onConnectSuccess(userName);
                    } else if ("auth_failure".equals(m.type)) {
                        listener.onAuthFailure(m.text);
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
                        listener.onUserListUpdate(listToSend, userName);
                    } else if (authenticated) {
                        if ("system".equals(m.type)) {
                            listener.onSystemMessage(m.text);

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
                                listener.onUserListUpdate(listToSend, userName);
                            }
                        } else {
                            listener.onMessageReceived(m);
                        }
                    }
                } catch (JsonSyntaxException ignore) {}
            }
        } catch (IOException ignore) {
        } finally {
            if (connected) {
                listener.onSystemMessage("Connection lost.");
                disconnect("Connection lost.");
            }
        }
    }

    public void sendMessage(String text, String recipient) throws IOException {
        if (!connected || !authenticated || out == null) throw new IOException("Not connected or authenticated.");

        Message msgToSend;
        if ("Public Chat".equals(recipient)) {
            msgToSend = com.chat.Message.chat(text);
        } else {
            msgToSend = com.chat.Message.direct(recipient, text);
        }

        String json = gson.toJson(msgToSend) + "\n";
        out.write(json.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}