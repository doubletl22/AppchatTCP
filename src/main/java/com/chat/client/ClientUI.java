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

public class ClientUI extends JFrame implements ClientStatusListener {
    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JTextField portField = new JTextField("5555");
    private final JButton connectBtn = new JButton("Kết nối");
    private final JButton disconnectBtn = new JButton("Ngắt kết nối");
    private final JLabel statusLabel = new JLabel("Trạng thái: Ngắt kết nối");

    private final DefaultListModel<String> conversationListModel = new DefaultListModel<>();
    private final JList<String> conversationList = new JList<>(conversationListModel);

    // THAY THẾ JTextArea bằng JPanel để tạo Chat Bubbles (bong bóng chat)
    private final JPanel chatDisplayPanel = new JPanel(new GridBagLayout());
    private final JTextField inputField = new JTextField();
    private final JButton sendBtn = new JButton("Gửi");
    private final JLabel chatHeaderLabel = new JLabel("Public Chat", SwingConstants.CENTER);

    private String userName = "User";
    private String currentRecipient = "Public Chat";
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    private final ChatClientCore clientCore;

    public ClientUI() {
        super("Chat Client ");
        this.clientCore = new ChatClientCore(this);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

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

        // Chat Area (Sử dụng JPanel với GridBagLayout)
        chatDisplayPanel.setBackground(Color.WHITE);
        JScrollPane chatScroll = new JScrollPane(chatDisplayPanel);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);
        chatScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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

    // --- Message Rendering Logic (Zalo-like Bubbles) ---

    private JPanel createChatBubble(String sender, String text, boolean isSelf) {
        // Wrapper for text content and optional name/timestamp
        JPanel bubblePanel = new JPanel();
        bubblePanel.setLayout(new BoxLayout(bubblePanel, BoxLayout.Y_AXIS));
        bubblePanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        bubblePanel.setOpaque(true);

        Color bgColor = isSelf ? new Color(0, 137, 255) : new Color(230, 230, 230); // Zalo Blue vs Grey
        Color fgColor = isSelf ? Color.WHITE : Color.BLACK;

        bubblePanel.setBackground(bgColor);

        // 1. Sender name
        if (!isSelf && !currentRecipient.equals("Public Chat")) {
            JLabel nameLabel = new JLabel(sender);
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 11f));
            nameLabel.setForeground(new Color(0, 102, 204));
            nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            bubblePanel.add(nameLabel);
        }

        // 2. Message Text
        JTextPane textPane = new JTextPane();
        textPane.setText(text);
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setBackground(null);
        textPane.setForeground(fgColor);
        textPane.setBorder(null);
        textPane.setFont(textPane.getFont().deriveFont(13f));

        // Ensure text wraps within a reasonable width for chat bubbles
        textPane.setPreferredSize(new Dimension(300, textPane.getPreferredSize().height));
        textPane.setMaximumSize(new Dimension(300, Integer.MAX_VALUE));

        bubblePanel.add(textPane);

        return bubblePanel;
    }

    private void addMessageToDisplay(String sender, String text, String type, boolean isSelf) {
        SwingUtilities.invokeLater(() -> {

            // 1. System/History Messages are treated as simple centered text
            if ("system".equals(type) || "history".equals(type) || text.startsWith("---") || text.startsWith("[AUTH FAILED]") || text.startsWith("Disconnected.")) {
                // Create a simple centered label for system messages
                String displayTime = "[" + timeFormat.format(new Date()) + "] ";
                String displayText = text;
                if ("history".equals(type) || "chat".equals(type)) { // Format history message text
                    displayText = displayTime + sender + ": " + text;
                } else if ("system".equals(type) || text.startsWith("---") || text.startsWith("[AUTH FAILED]")) {
                    displayText = displayTime + text;
                }

                JLabel systemLabel = new JLabel(displayText, SwingConstants.CENTER);
                systemLabel.setForeground(new Color(150, 150, 150));
                systemLabel.setFont(systemLabel.getFont().deriveFont(Font.ITALIC, 11f));
                systemLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

                GridBagConstraints gbc = createGBC(GridBagConstraints.CENTER);
                chatDisplayPanel.add(systemLabel, gbc);

            } else { // 2. Chat/DM messages are treated as bubbles
                JPanel messageBubble = createChatBubble(sender, text, isSelf);

                // Wrapper for alignment
                JPanel alignmentWrapper = new JPanel(new FlowLayout(isSelf ? FlowLayout.RIGHT : FlowLayout.LEFT, 10, 5));
                alignmentWrapper.setBackground(chatDisplayPanel.getBackground());
                alignmentWrapper.add(messageBubble);

                GridBagConstraints gbc = createGBC(isSelf ? GridBagConstraints.EAST : GridBagConstraints.WEST);

                chatDisplayPanel.add(alignmentWrapper, gbc);
            }

            // Ensure filler is at the bottom to push components to the top
            updateFiller();

            // Scroll to the bottom
            chatDisplayPanel.revalidate();
            chatDisplayPanel.repaint();
            JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, chatDisplayPanel);
            if (scrollPane != null) {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            }
        });
    }

    private GridBagConstraints createGBC(int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.anchor = anchor;
        gbc.insets = new Insets(1, 5, 1, 5);
        return gbc;
    }

    private void updateFiller() {
        // Add a vertical strut/filler component that pushes everything up
        GridBagConstraints fillerGBC = new GridBagConstraints();
        fillerGBC.gridwidth = GridBagConstraints.REMAINDER;
        fillerGBC.weighty = 1.0;
        fillerGBC.fill = GridBagConstraints.VERTICAL;

        Component verticalGlue = null;
        if (chatDisplayPanel.getComponentCount() > 0) {
            Component lastComponent = chatDisplayPanel.getComponent(chatDisplayPanel.getComponentCount() - 1);
            if (lastComponent instanceof Box.Filler) {
                verticalGlue = lastComponent;
                chatDisplayPanel.remove(verticalGlue);
            }
        }

        if (verticalGlue == null) {
            verticalGlue = Box.createVerticalGlue();
        }

        chatDisplayPanel.add(verticalGlue, fillerGBC);
    }

    // --- End Message Rendering Logic ---

    private void changeRecipient() {
        String selected = conversationList.getSelectedValue();
        if (selected == null || selected.equals(currentRecipient)) return;

        currentRecipient = selected;
        chatHeaderLabel.setText(currentRecipient);

        // Clear chat area
        chatDisplayPanel.removeAll();
        chatDisplayPanel.revalidate();
        chatDisplayPanel.repaint();

        if (currentRecipient.equals("Public Chat")) {
            addMessageToDisplay(null, "--- Chuyển sang Public Chat (Lịch sử đoạn chat không hiển thị) ---", "system", false);
        } else {
            addMessageToDisplay(null, "--- Chuyển sang tin nhắn trực tiếp tới " + currentRecipient + " ---", "system", false);
        }
    }

    // NOTE: Old 'appendChat' method is no longer used, all logging/messages go through addMessageToDisplay

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

        chatDisplayPanel.removeAll();
        statusLabel.setText("Status: Connecting...");
        addMessageToDisplay(null, "Attempting to connect and authenticate...", "system", false);

        try {
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
        clientCore.disconnect("User initiated disconnect.");
    }

    private void sendAction(ActionEvent e) {
        if (!clientCore.isAuthenticated()) return;
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.setText("");

        String recipient = currentRecipient;

        try {
            clientCore.sendMessage(text, recipient);

            // UI Update: Local echo (now uses bubble style)
            if ("Public Chat".equals(recipient) && currentRecipient.equals(recipient)) {
                addMessageToDisplay(userName, text, "chat", true);
            } else if (currentRecipient.equals(recipient)) {
                addMessageToDisplay(userName, text, "dm", true);
            }

        } catch (IOException ex) {
            addMessageToDisplay(null, "[ERROR] " + ex.getMessage(), "system", false);
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
        addMessageToDisplay(null, "Authentication successful. Welcome, " + userName + "!", "system", false);
    }

    @Override
    public void onDisconnect(String reason) {
        SwingUtilities.invokeLater(() -> {
            connectBtn.setEnabled(true);
            disconnectBtn.setEnabled(false);
            sendBtn.setEnabled(false);
            statusLabel.setText("Status: Disconnected");
            chatHeaderLabel.setText("Public Chat");
            chatDisplayPanel.removeAll();
            currentRecipient = "Public Chat";
            conversationListModel.clear();
            conversationListModel.addElement("Public Chat");
            conversationList.setSelectedIndex(0);
            addMessageToDisplay(null, "Disconnected. Reason: " + (reason != null && !reason.isEmpty() ? reason : "Unknown."), "system", false);
        });
    }

    @Override
    public void onAuthFailure(String reason) {
        addMessageToDisplay(null, "[AUTH FAILED] " + reason, "system", false);
    }

    @Override
    public void onSystemMessage(String text) {
        addMessageToDisplay(null, text, "system", false);
    }

    @Override
    public void onMessageReceived(Message m) {
        // Tin nhắn hệ thống/lịch sử đã được xử lý ở onSystemMessage, ngoại trừ History
        if ("chat".equals(m.type)) {
            if (currentRecipient.equals("Public Chat")) {
                // FIX: BỎ QUA tin nhắn chat công khai nếu người gửi là CHÍNH MÌNH.
                if (m.name.equals(userName)) {
                    return;
                }
                addMessageToDisplay(m.name, m.text, "chat", false);
            }
        } else if ("history".equals(m.type)) {
            // Hiển thị lịch sử dưới dạng tin nhắn hệ thống đơn giản
            if (currentRecipient.equals("Public Chat")) {
                addMessageToDisplay(m.name, m.text, "history", false);
            }
        } else if ("dm".equals(m.type)) {
            String conversationPartner = m.name.startsWith("[TO ") ? m.targetName : m.name;

            if (currentRecipient.equals(conversationPartner)) {
                if (m.name.startsWith("[TO ")) {
                    // FIX: BỎ QUA tin nhắn xác nhận DM từ server, vì nó đã được hiển thị qua Local Echo trong sendAction.
                    return;
                } else {
                    // Tin nhắn DM đến từ người khác (m.name không bắt đầu bằng [TO)
                    addMessageToDisplay(m.name, m.text, "dm", false);
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
                    chatDisplayPanel.removeAll();
                    addMessageToDisplay(null, "--- Người dùng bạn đang chat trực tiếp đã ngắt kết nối, chuyển sang public chat! ---", "system", false);
                }
            }
            addMessageToDisplay(null, "User list synchronized.", "system", false);
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