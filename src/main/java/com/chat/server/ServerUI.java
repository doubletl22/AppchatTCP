
package com.chat.server;

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

public class ServerUI extends JFrame {
    private final JTextField hostField = new JTextField("0.0.0.0");
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

    public ServerUI() {
        super("TCP Chat Server (Swing)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.add(new JLabel("Host:"));
        hostField.setColumns(12);
        top.add(hostField);
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
            appendLog("Server started on " + hostField.getText().trim() + ":" + port);

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

        ClientConn(Socket socket) throws IOException {
            super("client-" + socket.getRemoteSocketAddress());
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new BufferedOutputStream(socket.getOutputStream());
        }

        @Override
        public void run() {
            try {
                // Expect hello
                String line = reader.readLine();
                if (line == null) { close(); return; }
                Message hello = gson.fromJson(line, Message.class);
                if (hello == null || !"hello".equals(hello.type)) { close(); return; }
                name = hello.name != null ? hello.name : socket.getRemoteSocketAddress().toString();
                clients.add(this);
                refreshClientList();
                appendLog("Client connected: " + name + " @ " + socket.getRemoteSocketAddress());
                broadcast(Message.system(name + " joined the chat."));

                while (open) {
                    String l = reader.readLine();
                    if (l == null) break;
                    try {
                        Message m = gson.fromJson(l, Message.class);
                        if (m != null && "chat".equals(m.type)) {
                            Message outMsg = new Message();
                            outMsg.type = "chat";
                            outMsg.name = name;
                            outMsg.text = m.text;
                            broadcast(outMsg);
                        }
                    } catch (JsonSyntaxException ignore) {}
                }
            } catch (IOException e) {
                // ignore
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
