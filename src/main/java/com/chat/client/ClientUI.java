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
    // private final JTextField nameField = new JTextField("User"); // REMOVED
    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JTextField portField = new JTextField("5555");
    private final JButton connectBtn = new JButton("Connect");
    private final JButton disconnectBtn = new JButton("Disconnect");

    private final JTextArea chatArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final JButton sendBtn = new JButton("Send");
    private final JLabel statusLabel = new JLabel("Status: Disconnected"); // NEW: For displaying connected user

    private volatile boolean connected = false;
    private volatile boolean authenticated = false; // NEW: Authentication state
    private String userName = "User"; // NEW: Store authenticated username
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
        top.add(statusLabel); // NEW: Status label
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

    // NEW: Login/Register Dialog
    private class LoginDialog extends JDialog {
        private final JTextField usernameField = new JTextField(15);
        private final JPasswordField passwordField = new JPasswordField(15);
        private boolean cancelled = true;
        private String username;
        private String password;
        private String action; // "login" or "register"

        public LoginDialog(JFrame parent) {
            super(parent, "Login or Register", true);
            setLayout(new GridLayout(4, 2, 5, 5));
            ((JPanel)getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

            add(new JLabel("Username:"));
            add(usernameField);
            add(new JLabel("Password:"));
            add(passwordField);

            JButton loginBtn = new JButton("Login");
            JButton registerBtn = new JButton("Register");

            loginBtn.addActionListener(e -> {
                username = usernameField.getText();
                password = new String(passwordField.getPassword());
                action = "login";
                cancelled = false;
                setVisible(false);
            });

            registerBtn.addActionListener(e -> {
                username = usernameField.getText();
                password = new String(passwordField.getPassword());
                action = "register";
                cancelled = false;
                setVisible(false);
            });

            add(loginBtn);
            add(registerBtn);

            pack();
            setLocationRelativeTo(parent);
            setResizable(false);
        }

        public boolean isCancelled() { return cancelled; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getAction() { return action; }
    }


    private void connectAction(ActionEvent e) {
        if (connected) return;

        // 1. Show Login/Register Dialog
        LoginDialog dialog = new LoginDialog(this);
        dialog.setVisible(true);

        if (dialog.isCancelled()) return;

        final String username = dialog.getUsername().trim();
        final String password = dialog.getPassword().trim();
        final String action = dialog.getAction();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and Password cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

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

            // 2. Send Login or Register message
            Message authMsg = "login".equals(action)
                    ? Message.login(username, password)
                    : Message.register(username, password);

            String json = gson.toJson(authMsg) + "\n";
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Cannot connect: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        connected = true;
        connectBtn.setEnabled(false);
        disconnectBtn.setEnabled(true);
        statusLabel.setText("Status: Connecting..."); // MODIFIED
        appendChat("Attempting to connect and authenticate...");

        recvThread = new Thread(this::recvLoop, "recv-loop");
        recvThread.setDaemon(true);
        recvThread.start();
    }

    private void disconnectAction(ActionEvent e) {
        if (!connected) return;
        connected = false;
        authenticated = false;
        userName = "User";
        try { socket.close(); } catch (Exception ignored) {}
        socket = null;
        connectBtn.setEnabled(true);
        disconnectBtn.setEnabled(false);
        sendBtn.setEnabled(false);
        statusLabel.setText("Status: Disconnected"); // NEW
        appendChat("Disconnected.");
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
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Status: Logged in as " + userName);
                            sendBtn.setEnabled(true);
                        });
                        appendChat("Authentication successful. Welcome, " + userName + "!");
                    } else if ("auth_failure".equals(m.type)) {
                        appendChat("[AUTH FAILED] " + m.text);
                        // Force disconnect on auth failure
                        disconnectAction(null);
                        return; // Stop recvLoop
                    } else if ("history".equals(m.type)) { // NEW: Display chat history
                        // History messages are displayed with a special tag
                        appendChat("[HISTORY] " + m.name + ": " + m.text);
                    } else if (authenticated) {
                        // Only process chat/system messages if authenticated
                        if ("chat".equals(m.type)) {
                            appendChat(m.name + ": " + m.text);
                        } else if ("system".equals(m.type)) {
                            appendChat(m.text);
                        }
                    }
                } catch (Exception ignore) {}
            }
        } catch (IOException ignore) {
        } finally {
            if (connected) {
                appendChat("Connection lost.");
                SwingUtilities.invokeLater(() -> disconnectAction(null));
            }
        }
    }

    private void sendAction(ActionEvent e) {
        if (!connected || !authenticated || out == null) return; // Must be authenticated
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.setText("");
        try {
            // Client still sends the simple chat message
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