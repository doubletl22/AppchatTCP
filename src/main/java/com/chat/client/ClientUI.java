package com.chat.client;

import com.chat.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

// Import ChatClientCore và ClientStatusListener (Giả định nằm trong cùng package)

public class ClientUI extends JFrame implements ClientStatusListener {
    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JTextField portField = new JTextField("5555");
    private final JButton connectBtn = new JButton("Kết nối");
    private final JButton disconnectBtn = new JButton("Ngắt kết nối");
    private final JLabel statusLabel = new JLabel("Trạng thái: Ngắt kết nối");

    private final DefaultListModel<String> conversationListModel = new DefaultListModel<>();
    private final JList<String> conversationList = new JList<>(conversationListModel);

    private final JTextArea chatArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final JButton sendBtn = new JButton("Gửi");
    private final JLabel chatHeaderLabel = new JLabel("Public Chat", SwingConstants.CENTER);

    private String userName = "User";
    private String currentRecipient = "Public Chat";
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    // Core/Service Layer Instance
    private final ChatClientCore clientCore;

    public ClientUI() {
        super("Chat Client ");
        // Initialize Core Layer, passing self as the listener
        this.clientCore = new ChatClientCore(this);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        // ... (UI setup code)

        // --- TOP PANEL (Connection & Status) ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        topPanel.setBorder(new EmptyBorder(0, 10, 0, 10));
        topPanel.add(statusLabel);
        topPanel.add(new JLabel("Host:"));
        hostField.setColumns(8);
        topPanel.add(hostField);
        topPanel.add(new JLabel("Port:"));
        portField.setColumns(4);
        topPanel.add(portField);
        connectBtn.addActionListener(this::connectAction);
        disconnectBtn.addActionListener(this::disconnectAction);
        disconnectBtn.setEnabled(false);
        topPanel.add(connectBtn);
        topPanel.add(disconnectBtn);

        // --- MAIN CONTENT AREA (JSplitPane) ---
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(250);
        mainSplit.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 1. LEFT PANEL (User/Conversation List)
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("Conversations", SwingConstants.CENTER), BorderLayout.NORTH);

        conversationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        conversationList.setSelectedIndex(0);

        conversationList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    changeRecipient();
                }
            }
        });

        JScrollPane listScroll = new JScrollPane(conversationList);
        leftPanel.add(listScroll, BorderLayout.CENTER);
        mainSplit.setLeftComponent(leftPanel);

        // 2. RIGHT PANEL (Active Chat)
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));

        chatHeaderLabel.setFont(chatHeaderLabel.getFont().deriveFont(Font.BOLD, 14f));
        chatPanel.add(chatHeaderLabel, BorderLayout.NORTH);

        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        JPanel bottomInput = new JPanel(new BorderLayout(5, 5));
        inputField.addActionListener(e -> sendAction(null));
        sendBtn.addActionListener(this::sendAction);
        sendBtn.setEnabled(false);
        bottomInput.add(inputField, BorderLayout.CENTER);
        bottomInput.add(sendBtn, BorderLayout.EAST);
        chatPanel.add(bottomInput, BorderLayout.SOUTH);

        mainSplit.setRightComponent(chatPanel);

        // --- ROOT CONTAINER ---
        JPanel root = new JPanel(new BorderLayout());
        root.add(topPanel, BorderLayout.NORTH);
        root.add(mainSplit, BorderLayout.CENTER);
        setContentPane(root);

        conversationListModel.addElement("Public Chat");
    }

    private void changeRecipient() {
        String selected = conversationList.getSelectedValue();
        if (selected == null || selected.equals(currentRecipient)) return;

        currentRecipient = selected;
        chatHeaderLabel.setText(currentRecipient);

        chatArea.setText("");
        if (currentRecipient.equals("Public Chat")) {
            appendChat("--- Chuyển sang Public Chat (Lịch sử đoạn chat không hiển thị) ---");
        } else {
            appendChat("--- Chuyển sang tin nhắn trực tiếp tới " + currentRecipient + " ---");
        }
    }

    private void appendChat(String s) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append("[" + timeFormat.format(new Date()) + "] " + s + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    // LoginDialog class (remains the same)
    private class LoginDialog extends JDialog {
        private final JTextField usernameField = new JTextField(15);
        private final JPasswordField passwordField = new JPasswordField(15);
        private boolean cancelled = true;
        private String username;
        private String password;
        private String action;

        public LoginDialog(JFrame parent) {
            super(parent, "Đăng nhập hoặc đăng kí", true);
            setLayout(new GridLayout(4, 2, 5, 5));
            ((JPanel)getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

            add(new JLabel("Tên đăng nhập:"));
            add(usernameField);
            add(new JLabel("Mật khẩu:"));
            add(passwordField);

            JButton loginBtn = new JButton("Đăng nhập");
            JButton registerBtn = new JButton("Đăng kí");

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
        if (clientCore.isConnected()) return;

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

        chatArea.setText("");
        statusLabel.setText("Status: Connecting...");
        appendChat("Attempting to connect and authenticate...");

        try {
            // DELEGATE to Core Layer
            clientCore.connectAndAuth(host, port, username, password, action);
            connectBtn.setEnabled(false);
            disconnectBtn.setEnabled(true);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Cannot connect: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            connectBtn.setEnabled(true);
            disconnectBtn.setEnabled(false);
        }
    }

    private void disconnectAction(ActionEvent e) {
        // DELEGATE to Core Layer
        clientCore.disconnect("User initiated disconnect.");
    }

    private void sendAction(ActionEvent e) {
        if (!clientCore.isAuthenticated()) return;
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.setText("");

        String recipient = currentRecipient;

        try {
            // DELEGATE to Core Layer
            clientCore.sendMessage(text, recipient);

            // UI Update: Local echo
            if ("Public Chat".equals(recipient) && currentRecipient.equals(recipient)) {
                appendChat(userName + ": " + text);
            } else if (currentRecipient.equals(recipient)) {
                appendChat("Tin nhắn gửi tới " + recipient + "] " + text);
            }

        } catch (IOException ex) {
            appendChat("[ERROR] " + ex.getMessage());
            clientCore.disconnect(null);
        }
    }

    // --- IMPLEMENTATION OF ClientStatusListener (Receives events from Core Layer) ---
    @Override
    public void onConnectSuccess(String userName) {
        SwingUtilities.invokeLater(() -> {
            this.userName = userName;
            statusLabel.setText("Status: Logged in as " + userName);
            sendBtn.setEnabled(true);
        });
        appendChat("Authentication successful. Welcome, " + userName + "!");
    }

    @Override
    public void onDisconnect(String reason) {
        SwingUtilities.invokeLater(() -> {
            connectBtn.setEnabled(true);
            disconnectBtn.setEnabled(false);
            sendBtn.setEnabled(false);
            statusLabel.setText("Status: Disconnected");
            chatHeaderLabel.setText("Public Chat");
            chatArea.setText("");
            currentRecipient = "Public Chat";
            conversationListModel.clear();
            conversationListModel.addElement("Public Chat");
            conversationList.setSelectedIndex(0);
            appendChat("Disconnected. Reason: " + (reason != null && !reason.isEmpty() ? reason : "Unknown."));
        });
    }

    @Override
    public void onAuthFailure(String reason) {
        appendChat("[AUTH FAILED] " + reason);
    }

    @Override
    public void onSystemMessage(String text) {
        appendChat(text);
    }

    @Override
    public void onMessageReceived(Message m) {
        if ("chat".equals(m.type)) {
            if (currentRecipient.equals("Public Chat")) {
                appendChat(m.name + ": " + m.text);
            }
        } else if ("history".equals(m.type)) {
            if (currentRecipient.equals("Public Chat")) {
                appendChat("[HISTORY] " + m.name + ": " + m.text);
            }
        } else if ("dm".equals(m.type)) {
            String conversationPartner = m.name.startsWith("[TO ") ? m.targetName : m.name;

            if (currentRecipient.equals(conversationPartner)) {
                if (m.name.startsWith("[TO ")) {
                    appendChat("Tin nhắn đã gửi tới " + m.targetName + "] " + m.text);
                } else {
                    appendChat("[DM từ " + m.name + "] " + m.text);
                }
            }
        }
    }

    @Override
    public void onUserListUpdate(List<String> userNames, String selfName) {
        SwingUtilities.invokeLater(() -> {
            conversationListModel.clear();
            conversationListModel.addElement("Public Chat");

            userNames.forEach(conversationListModel::addElement);

            if (currentRecipient.equals("Public Chat")) {
                conversationList.setSelectedIndex(0);
            } else {
                int index = conversationListModel.indexOf(currentRecipient);
                if (index >= 0) {
                    conversationList.setSelectedIndex(index);
                } else {
                    currentRecipient = "Public Chat";
                    conversationList.setSelectedIndex(0);
                    chatHeaderLabel.setText("Public Chat");
                    chatArea.setText("");
                    appendChat("--- Người dùng bạn đang chat trực tiếp đã ngắt kết nối, chuyển sang public chat! ---");
                }
            }
            appendChat("User list synchronized.");
        });
    }


    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf: " + ex);
        }
        SwingUtilities.invokeLater(() -> new ClientUI().setVisible(true));
    }
}