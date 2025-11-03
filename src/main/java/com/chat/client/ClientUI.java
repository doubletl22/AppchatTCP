
package com.chat.client;

import com.chat.Message;
import com.google.gson.Gson;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientUI extends JFrame {
    private final JTextField nameField = new JTextField("User");
    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JTextField portField = new JTextField("5555");
    private final JButton connectBtn = new JButton("Connect");
    private final JButton disconnectBtn = new JButton("Disconnect");

    private final JTextArea chatArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final JButton sendBtn = new JButton("Send");

    private volatile boolean connected = false;
    private Socket socket;
    private BufferedReader reader;
    private BufferedOutputStream out;
    private Thread recvThread;
    private final Gson gson = new Gson();

    public ClientUI() {
        super("TCP Chat Client (Swing)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 560);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.add(new JLabel("Name:"));
        nameField.setColumns(10);
        top.add(nameField);
        top.add(new JLabel("Host:"));
        hostField.setColumns(12);
        top.add(hostField);
        top.add(new JLabel("Port:"));
        portField.setColumns(6);
        top.add(portField);
        connectBtn.addActionListener(this::connectAction);
        disconnectBtn.addActionListener(this::disconnectAction);
        disconnectBtn.setEnabled(false);
        top.add(connectBtn);
        top.add(disconnectBtn);
        root.add(top, BorderLayout.NORTH);

        chatArea.setEditable(false);
        root.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        inputField.addActionListener(e -> sendAction(null));
        sendBtn.addActionListener(this::sendAction);
        sendBtn.setEnabled(false);
        bottom.add(inputField, BorderLayout.CENTER);
        bottom.add(sendBtn, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);
    }

    private void appendChat(String s) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + s + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void connectAction(ActionEvent e) {
        if (connected) return;
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = "User";
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Port must be integer", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new BufferedOutputStream(socket.getOutputStream());

            String hello = new Gson().toJson(Message.hello(name)) + "\n";
            out.write(hello.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Cannot connect: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        connected = true;
        connectBtn.setEnabled(false);
        disconnectBtn.setEnabled(true);
        sendBtn.setEnabled(true);
        appendChat("Connected.");

        recvThread = new Thread(this::recvLoop, "recv-loop");
        recvThread.setDaemon(true);
        recvThread.start();
    }

    private void disconnectAction(ActionEvent e) {
        if (!connected) return;
        connected = false;
        try { socket.close(); } catch (Exception ignored) {}
        socket = null;
        connectBtn.setEnabled(true);
        disconnectBtn.setEnabled(false);
        sendBtn.setEnabled(false);
        appendChat("Disconnected.");
    }

    private void recvLoop() {
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                try {
                    Message m = gson.fromJson(line, Message.class);
                    if ("chat".equals(m.type)) {
                        appendChat(m.name + ": " + m.text);
                    } else if ("system".equals(m.type)) {
                        appendChat(m.text);
                    }
                } catch (Exception ignore) {}
            }
        } catch (IOException ignore) {
        } finally {
            if (connected) {
                appendChat("Connection lost.");
                connected = false;
                SwingUtilities.invokeLater(() -> {
                    connectBtn.setEnabled(true);
                    disconnectBtn.setEnabled(false);
                    sendBtn.setEnabled(false);
                });
            }
        }
    }

    private void sendAction(ActionEvent e) {
        if (!connected || out == null) return;
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.setText("");
        try {
            String json = gson.toJson(Message.chat(text)) + "\n";
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException ex) {
            appendChat("[ERROR] " + ex.getMessage());
            disconnectAction(null);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientUI().setVisible(true));
    }
}
