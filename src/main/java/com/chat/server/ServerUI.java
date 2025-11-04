package com.chat.server;

import com.chat.DatabaseManager; // NEW
import com.chat.Message;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ServerUI extends JFrame {
    // private final JTextField hostField = new JTextField("0.0.0.0"); // REMOVED
    private final JTextField portField = new JTextField("5555");
    private final JButton startBtn = new JButton("Start");
    private final JButton stopBtn = new JButton("Stop");
    private final JTextArea logArea = new JTextArea();
    private final DefaultListModel<String> clientListModel = new DefaultListModel<>();
    private final JList<String> clientList = new JList<>(clientListModel);
    private final JTextField broadcastField = new JTextField();
    private final JButton broadcastBtn = new JButton("Broadcast");
    private final JButton kickBtn = new JButton("Kick Selected");

    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private final List<ClientConn> clients = new CopyOnWriteArrayList<>();
    private final Gson gson = new Gson();
    private final DatabaseManager dbManager = new DatabaseManager(); // NEW: Database Manager

    public ServerUI() {
        super("TCP Chat Server (Swing)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.add(new JLabel("Port:"));
        portField.setColumns(6);
        top.add(portField);
        startBtn.addActionListener(this::startServer);
        stopBtn.addActionListener(this::stopServer);
        stopBtn.setEnabled(false);
        top.add(startBtn);
        top.add(stopBtn);
        root.add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.7);
        root.add(split, BorderLayout.CENTER);

        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        split.setLeftComponent(logScroll);

        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.add(new JLabel("Connected Clients"), BorderLayout.NORTH);
        right.add(new JScrollPane(clientList), BorderLayout.CENTER);

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        kickBtn.addActionListener(e -> kickSelected());
        kickBtn.setEnabled(false);
        rightBtns.add(kickBtn);
        right.add(rightBtns, BorderLayout.SOUTH);

        split.setRightComponent(right);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.add(broadcastField, BorderLayout.CENTER);
        broadcastBtn.addActionListener(e -> broadcastFromServer());
        broadcastBtn.setEnabled(false);
        bottom.add(broadcastBtn, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);
    }

    private void startServer(ActionEvent e) {
        if (running) return;
        try {
            int port = Integer.parseInt(portField.getText().trim());
            serverSocket = new ServerSocket(port);
            running = true;
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            broadcastBtn.setEnabled(true);
            kickBtn.setEnabled(true);
            appendLog("Server started on 0.0.0.0:" + port + ". Ready for authentication."); // MODIFIED

            Thread acceptThread = new Thread(this::acceptLoop, "accept-loop");
            acceptThread.setDaemon(true);
            acceptThread.start();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Start server failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer(ActionEvent e) {
        if (!running) return;
        running = false;
        try {
            for (ClientConn c : new ArrayList<>(clients)) {
                c.close();
            }
            clients.clear();
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {}
        serverSocket = null;
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        broadcastBtn.setEnabled(false);
        kickBtn.setEnabled(false);
        clientListModel.clear();
        appendLog("Server stopped.");
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket sock = serverSocket.accept();
                ClientConn conn = new ClientConn(sock);
                conn.start();
            } catch (IOException ex) {
                if (running) appendLog("Accept error: " + ex.getMessage());
            }
        }
    }

    private void broadcastFromServer() {
        String text = broadcastField.getText().trim();
        if (text.isEmpty()) return;
        broadcastField.setText("");
        broadcast(Message.system("[SERVER]: " + text));
    }

    private void kickSelected() {
        String selected = clientList.getSelectedValue();
        if (selected == null) return;
        for (ClientConn c : clients) {
            if (selected.equals(c.name)) {
                c.close();
                appendLog("Kicked: " + selected);
                broadcast(Message.system(selected + " was kicked by server."));
                break;
            }
        }
    }

    private void broadcast(Message m) {
        // NEW: Store chat messages in database
        if ("chat".equals(m.type)) {
            dbManager.storeMessage(m.name, m.text);
        }

        String json = gson.toJson(m) + "\n";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        for (ClientConn c : new ArrayList<>(clients)) {
            try {
                c.out.write(bytes);
                c.out.flush();
            } catch (IOException ex) {
                c.close();
            }
        }
        if ("chat".equals(m.type)) {
            appendLog(m.name + ": " + m.text);
        } else if ("system".equals(m.type)) {
            appendLog(m.text);
        }
    }
    private void sendPrivateMessage(ClientConn sender, Message m) {
        String targetName = m.targetName;
        ClientConn target = null;
        for (ClientConn c : clients) {
            if (targetName.equals(c.name)) {
                target = c;
                break;
            }
        }

        if (target == null) {
            sendToClient(sender, Message.system("Người dùng" + targetName + "' không kết nối hoặc không tồn tại"));
            return;
        }
        // 1. Prepare message for target (show who sent it)
        Message msgToTarget = new Message();
        msgToTarget.type = "dm";
        msgToTarget.name = sender.name;
        msgToTarget.targetName = targetName;
        msgToTarget.text = m.text;

        // 2. Prepare confirmation message for sender (indicate message sent)
        Message msgToSender = new Message();
        msgToSender.type = "dm";
        msgToSender.name = "[TO " + targetName + "]"; // Tag for client to display "Sent to..."
        msgToSender.targetName = targetName;
        msgToSender.text = m.text;

        // 3. Send
        sendToClient(target, msgToTarget);
        sendToClient(sender, msgToSender);

        appendLog("[DM] " + sender.name + " -> " + targetName + ": " + m.text);
    }


    // NEW: Helper to send a single message to one client
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

    private void appendLog(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + new java.text.SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + s + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void refreshClientList() {
        SwingUtilities.invokeLater(() -> {
            clientListModel.clear();
            for (ClientConn c : clients) clientListModel.addElement(c.name);
        });
    }

    private class ClientConn extends Thread {
        final Socket socket;
        final BufferedReader reader;
        final BufferedOutputStream out;
        String name = null;
        volatile boolean open = true;
        volatile boolean authenticated = false; // NEW: Authentication state

        ClientConn(Socket socket) throws IOException {
            super("client-" + socket.getRemoteSocketAddress());
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new BufferedOutputStream(socket.getOutputStream());
        }

        @Override
        public void run() {
            try {
                // Wait for login or register message (Authentication Loop)
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
                            appendLog("User registered: " + m.username);
                        } else {
                            sendToClient(this, Message.authFailure("Username already exists or registration failed."));
                        }
                    } else if ("login".equals(m.type)) {
                        if (dbManager.authenticateUser(m.username, m.password)) {
                            name = m.username;
                            authenticated = true;
                            sendToClient(this, Message.authSuccess(name));
                            appendLog("Client authenticated: " + name + " @ " + socket.getRemoteSocketAddress());

                            // Send chat history (up to 100 latest messages)
                            List<Message> history = dbManager.getChatHistory(100);
                            for (Message chatMsg : history) {
                                sendToClient(this, chatMsg);
                            }
                            sendToClient(this, Message.system("Chat history loaded."));

                            clients.add(this);
                            refreshClientList();

                            List<String> currentNames = clients.stream()
                                    .map(c -> c.name)
                                    .filter(n -> !n.equals(name))
                                    .collect(Collectors.toList());
                            sendToClient(this, Message.userlist(currentNames));



                            broadcast(Message.system(name + " joined the chat."));
                            break; // Exit authentication loop
                        } else {
                            sendToClient(this, Message.authFailure("Login failed: Invalid username or password."));
                            appendLog("Login attempt failed for: " + m.username);
                        }
                    } else {
                        sendToClient(this, Message.authFailure("Must login or register first."));
                    }
                } // End of authentication loop

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
                                broadcast(outMsg); // Public Chat
                            } else if ("dm".equals(m.type) && m.targetName != null) { // NEW: Handle DM
                                m.name = name; // Set sender's name from connection
                                sendPrivateMessage(this, m); // Private Chat
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
            refreshClientList();
            if (name != null) {
                broadcast(Message.system(name + " left the chat."));
                appendLog("Client disconnected: " + name);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerUI().setVisible(true));
    }
}