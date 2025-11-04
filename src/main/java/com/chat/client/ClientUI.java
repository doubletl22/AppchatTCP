package com.chat.client;

import com.chat.Message;
import com.google.gson.Gson;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class ClientUI extends JFrame {
    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JTextField portField = new JTextField("5555");
    private final JButton connectBtn = new JButton("Connect");
    private final JButton disconnectBtn = new JButton("Disconnect");
    private final JLabel statusLabel = new JLabel("Status: Disconnected");

    // NEW UI COMPONENTS for Zalo-like layout
    private final DefaultListModel<String> conversationListModel = new DefaultListModel<>(); // Danh sách trò chuyện
    private final JList<String> conversationList = new JList<>(conversationListModel);

    // Main Chat Panel components
    private final JTextArea chatArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final JButton sendBtn = new JButton("Send");
    private final JLabel chatHeaderLabel = new JLabel("Public Chat", SwingConstants.CENTER); // Hiển thị tên người/nhóm đang chat

    private volatile boolean connected = false;
    private volatile boolean authenticated = false;
    private String userName = "User";
    private final List<String> currentUsers = new ArrayList<>(); // To track all connected users for the list
    private String currentRecipient = "Public Chat"; // The active conversation recipient

    private Socket socket;
    private BufferedReader reader;
    private BufferedOutputStream out;
    private Thread recvThread;
    private final Gson gson = new Gson();

    public ClientUI() {
        super("TCP Chat Client (Swing)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650); // Mở rộng kích thước
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
        mainSplit.setDividerLocation(250); // Chiều rộng cho danh sách trò chuyện
        mainSplit.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 1. LEFT PANEL (User/Conversation List)
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("Conversations", SwingConstants.CENTER), BorderLayout.NORTH);

        conversationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        conversationList.setSelectedIndex(0);

        // Listener để chuyển đổi cuộc trò chuyện khi click
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

        // Chat Header
        chatHeaderLabel.setFont(chatHeaderLabel.getFont().deriveFont(Font.BOLD, 14f));
        chatPanel.add(chatHeaderLabel, BorderLayout.NORTH);

        // Chat Area
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        // Input Panel
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

        // Khởi tạo danh sách mặc định
        conversationListModel.addElement("Public Chat");
    }

    // Xử lý chuyển đổi người nhận
    private void changeRecipient() {
        String selected = conversationList.getSelectedValue();
        if (selected == null || selected.equals(currentRecipient)) return;

        currentRecipient = selected;
        chatHeaderLabel.setText(currentRecipient);

        // Xóa màn hình chat cũ (Trong ứng dụng thực tế, bạn sẽ tải lịch sử chat ở đây)
        chatArea.setText("");
        if (currentRecipient.equals("Public Chat")) {
            appendChat("--- Switched to Public Chat (History not reloaded) ---");
        } else {
            appendChat("--- Switched to Direct Message with " + currentRecipient + " ---");
        }
    }

    // Cập nhật danh sách người dùng trong JList
    private void updateUserList(String name, boolean isJoining) {
        SwingUtilities.invokeLater(() -> {
            // Xử lý list of all connected users (currentUsers)
            if (isJoining && !currentUsers.contains(name)) {
                currentUsers.add(name);
            } else if (!isJoining) {
                currentUsers.remove(name);
            }

            // Cập nhật JList Model
            conversationListModel.clear();
            conversationListModel.addElement("Public Chat"); // Luôn có chat chung

            // Lọc ra chính mình và thêm vào list
            currentUsers.stream()
                    .sorted()
                    .filter(n -> !n.equals(userName))
                    .forEach(conversationListModel::addElement);

            // Duy trì lựa chọn hiện tại hoặc chuyển về Public Chat nếu người đang chat bị ngắt kết nối
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
                    appendChat("--- Your recipient disconnected. Switched to Public Chat ---");
                }
            }
        });
    }

    private void appendChat(String s) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + s + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    // LoginDialog class (Giữ nguyên)
    private class LoginDialog extends JDialog {
        private final JTextField usernameField = new JTextField(15);
        private final JPasswordField passwordField = new JPasswordField(15);
        private boolean cancelled = true;
        private String username;
        private String password;
        private String action;

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
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Cannot connect: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        connected = true;
        connectBtn.setEnabled(false);
        disconnectBtn.setEnabled(true);
        statusLabel.setText("Status: Connecting...");
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
        currentUsers.clear();
        currentRecipient = "Public Chat";
        try { socket.close(); } catch (Exception ignored) {}
        socket = null;
        connectBtn.setEnabled(true);
        disconnectBtn.setEnabled(false);
        sendBtn.setEnabled(false);
        statusLabel.setText("Status: Disconnected");
        chatHeaderLabel.setText("Public Chat");
        chatArea.setText("");
        conversationListModel.clear();
        conversationListModel.addElement("Public Chat");
        conversationList.setSelectedIndex(0);
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
                        currentUsers.add(userName);
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Status: Logged in as " + userName);
                            sendBtn.setEnabled(true);
                            updateUserList(null, false);
                        });
                        appendChat("Authentication successful. Welcome, " + userName + "!");
                    } else if ("auth_failure".equals(m.type)) {
                        appendChat("[AUTH FAILED] " + m.text);
                        disconnectAction(null);
                        return;
                    } else if ("history".equals(m.type)) {
                        // Chỉ hiển thị lịch sử nếu đang xem Public Chat
                        if (currentRecipient.equals("Public Chat")) {
                            appendChat("[HISTORY] " + m.name + ": " + m.text);
                        }
                    } else if ("dm".equals(m.type) && authenticated) {
                        String senderOrRecipient = m.name.startsWith("[TO ") ? m.targetName : m.name;

                        // Chỉ hiển thị DM nếu đang xem đúng cuộc trò chuyện
                        if (currentRecipient.equals(senderOrRecipient)) {
                            if (m.name.startsWith("[TO ")) {
                                appendChat("[DM SENT to " + m.targetName + "] " + m.text);
                            } else {
                                appendChat("[DM RECEIVED from " + m.name + "] " + m.text);
                            }
                        } else {
                            // TODO: Thêm logic thông báo tin nhắn mới ở đây
                        }
                    } else if (authenticated) {
                        if ("chat".equals(m.type)) {
                            // Chỉ hiển thị tin nhắn công cộng nếu Public Chat đang được chọn
                            if (currentRecipient.equals("Public Chat")) {
                                appendChat(m.name + ": " + m.text);
                            }
                        } else if ("system".equals(m.type)) {
                            appendChat(m.text);

                            // Cập nhật danh sách người dùng khi có sự kiện tham gia/rời đi
                            if (m.text.endsWith(" joined the chat.")) {
                                String name = m.text.substring(0, m.text.indexOf(" joined the chat."));
                                updateUserList(name, true);
                            } else if (m.text.endsWith(" left the chat.") || m.text.endsWith(" was kicked by server.")) {
                                String name = m.text.substring(0, m.text.indexOf(" left the chat."));
                                if (name.equals(m.text)) name = m.text.substring(0, m.text.indexOf(" was kicked by server."));
                                updateUserList(name, false);
                            }
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
        if (!connected || !authenticated || out == null) return;
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;
        inputField.setText("");

        String recipient = currentRecipient;

        try {
            Message msgToSend;
            if ("Public Chat".equals(recipient)) {
                msgToSend = com.chat.Message.chat(text);
                // Hiển thị ngay tin nhắn chat công cộng
                if (currentRecipient.equals("Public Chat")) {
                    appendChat(userName + ": " + text);
                }
            } else {
                msgToSend = com.chat.Message.direct(recipient, text);
            }

            String json = gson.toJson(msgToSend) + "\n";
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.flush();

        } catch (IOException ex) {
            appendChat("[ERROR] " + ex.getMessage());
            disconnectAction(null);
        }
    }

    public static void main(String[] args) {
        // FlatLaf setup for modern look
        try {
            // Thiết lập FlatLightLaf
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf: " + ex);
        }
        SwingUtilities.invokeLater(() -> new ClientUI().setVisible(true));
    }
}