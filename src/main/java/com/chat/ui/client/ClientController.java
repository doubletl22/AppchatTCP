package com.chat.ui.client;

import com.chat.core.ChatClientCore;
import com.chat.core.ClientStatusListener;
import com.chat.model.ClientViewModel;
import com.chat.model.Message;
import com.chat.ui.client.dialog.LoginDialog;
import com.chat.util.UiUtils;

import javax.swing.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Controller xử lý tất cả Logic và là Listener cho Core Layer.
 */
public class ClientController implements ClientStatusListener {

    private final ChatClientCore clientCore;
    private final ClientViewModel viewModel;
    private final JFrame parentFrame;

    public ClientController(JFrame parentFrame, ChatClientCore clientCore, ClientViewModel viewModel) {
        this.parentFrame = parentFrame;
        this.clientCore = clientCore;
        this.viewModel = viewModel;
    }

    // --- Actions/Command Handlers ---

    public void handleConnect(String host, int port, String username, String password, String action) {
        if (clientCore.isConnected()) return;

        viewModel.setConnectionStatus(true, false);
        viewModel.notifyMessageReceived(Message.system("Attempting to connect and authenticate..."));

        try {
            clientCore.connectAndAuth(host, port, username, password, action);

        } catch (IOException ex) {
            viewModel.notifyMessageReceived(Message.system("[ERROR] Cannot connect: " + ex.getMessage()));
            viewModel.setConnectionStatus(false, false);
            JOptionPane.showMessageDialog(parentFrame, "Cannot connect: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void handleDisconnect() {
        clientCore.disconnect("User initiated disconnect.");
    }

    public void handleSend(String text) {
        if (!clientCore.isAuthenticated()) return;

        String recipient = viewModel.getCurrentRecipient();
        String userName = viewModel.getUserName();
        // NEW: Check for /gif command
        boolean isGif = text.trim().toLowerCase().startsWith("/gif ");

        try {
            if (isGif) {
                // Extract GIF keyword/URL (e.g., from "/gif hello" -> "hello")
                String gifText = text.trim().substring(5).trim();

                if (gifText.isEmpty()) {
                    viewModel.notifyMessageReceived(Message.system("[LỖI] Cú pháp GIF: /gif <từ_khóa>"));
                    return;
                }

                clientCore.sendGif(gifText, recipient); // Use the new sendGif method

                // Local Echo for GIF: Sử dụng marker [GIF]: để Message.java tạo đúng loại tin nhắn hiển thị
                if ("Public Chat".equals(recipient)) {
                    viewModel.notifyMessageReceived(Message.chat(userName, "[GIF]: " + gifText));
                } else {
                    viewModel.notifyMessageReceived(Message.dm(userName, recipient, "[GIF]: " + gifText, true));
                }
            } else {
                // Handle standard text message
                clientCore.sendMessage(text, recipient);

                // Local Echo for Text
                if ("Public Chat".equals(recipient)) {
                    viewModel.notifyMessageReceived(Message.chat(userName, text));
                } else {
                    // Tin nhắn DM (isSelf = true)
                    viewModel.notifyMessageReceived(Message.dm(userName, recipient, text, true));
                }
            }

        } catch (IOException ex) {
            viewModel.notifyMessageReceived(Message.system("[ERROR] " + ex.getMessage()));
            handleDisconnect();
        }
    }

    public void requestHistory(String targetUser) {
        try {
            clientCore.requestDirectHistory(targetUser);
            viewModel.notifyMessageReceived(Message.system("Đang tải lịch sử tin nhắn..."));
        } catch (IOException ex) {
            viewModel.notifyMessageReceived(Message.system("[ERROR] Không thể yêu cầu lịch sử DM: " + ex.getMessage()));
        }
    }

    public void showLoginDialog() {
        LoginDialog dialog = new LoginDialog(parentFrame);
        dialog.setVisible(true);

        if (dialog.isCancelled()) return;

        String host = dialog.getHost().trim();
        int port = dialog.getPort(); // <--- ĐÃ SỬA: Sử dụng dialog.getPort()

        final String username = dialog.getUsername().trim();
        final String password = dialog.getPassword().trim();
        final String action = dialog.getAction();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(parentFrame, "Username and Password cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Kiểm tra lỗi Port (được đánh dấu là -1 nếu parsing thất bại trong LoginDialog)
        if (port == -1) {
            JOptionPane.showMessageDialog(parentFrame, "Port must be integer", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        handleConnect(host, port, username, password, action);
    }

    // --- Core Listener Implementations (Callbacks từ ChatClientCore) ---

    @Override
    public void onConnectSuccess(String userName) {
        viewModel.setUserName(userName);
        viewModel.setConnectionStatus(true, true);
        viewModel.notifyMessageReceived(Message.system("Authentication successful. Welcome, " + userName + "!"));
    }

    @Override
    public void onDisconnect(String reason) {
        viewModel.setConnectionStatus(false, false);
        viewModel.updateUsers(Collections.emptyList());
        viewModel.setCurrentRecipient("Public Chat");
        viewModel.notifyMessageReceived(Message.system("Disconnected. Reason: " + (reason != null && !reason.isEmpty() ? reason : "Unknown.")));
    }

    @Override
    public void onAuthFailure(String reason) {
        viewModel.notifyMessageReceived(Message.system("[AUTH FAILED] " + reason));
        handleDisconnect();
    }

    @Override
    public void onSystemMessage(String text) {
        viewModel.notifyMessageReceived(Message.system(text));

        // Cập nhật danh sách người dùng dựa trên tin nhắn hệ thống (Logic Business)
        if (text.endsWith(" joined the chat.")) {
            String name = text.substring(0, text.indexOf(" joined the chat."));
            viewModel.addUser(name);
        } else if (text.endsWith(" left the chat.") || text.endsWith(" was kicked by server.")) {
            String name = text.substring(0, text.indexOf(" left the chat."));
            if (name.equals(text)) name = text.substring(0, text.indexOf(" was kicked by server."));
            viewModel.removeUser(name);
        }
    }

    @Override
    public void onUserListUpdate(List<String> userNames, String selfName) {
        viewModel.updateUsers(userNames);
        viewModel.notifyMessageReceived(Message.system("User list synchronized."));
    }

    @Override
    public void onMessageReceived(Message m) {
        // Xử lý logic chống lặp tin nhắn và thông báo cho View

        // NEW: Check for chat and gif types
        if (("chat".equals(m.type) || "gif".equals(m.type)) && m.name.equals(viewModel.getUserName())) {
            return; // Bỏ qua tin nhắn chat/gif công khai do chính mình gửi (đã Local Echo)
        }

        // NEW: Check for dm and dm_gif types (Local Echo Confirmation)
        if (("dm".equals(m.type) || "dm_gif".equals(m.type)) && m.name.startsWith("[TO ")) {
            return; // Bỏ qua xác nhận DM/DM GIF từ server (đã Local Echo)
        }

        viewModel.notifyMessageReceived(m);

        // =======================================================
        // NEW: LOGIC HIỂN THỊ THÔNG BÁO KHI NHẬN TIN NHẮN
        // =======================================================
        UiUtils.invokeLater(() -> {
            String senderName = m.name != null ? m.name : "Hệ thống";
            String title = "";
            String message = "";
            boolean isNewMessage = false;

            if ("chat".equals(m.type) || "gif".equals(m.type)) {
                title = "Tin nhắn công khai mới";
                // Lấy tối đa 100 ký tự đầu của tin nhắn
                message = senderName + ": " + (m.text.length() > 100 ? m.text.substring(0, 100) + "..." : m.text);
                isNewMessage = true;
            } else if ("dm".equals(m.type) || "dm_gif".equals(m.type)) {
                title = "Tin nhắn riêng mới từ " + senderName;
                // Lấy tối đa 100 ký tự đầu của tin nhắn
                message = m.text.length() > 100 ? m.text.substring(0, 100) + "..." : m.text;
                isNewMessage = true;
            }

            if (isNewMessage) {
                // 1. Kiểm tra nếu tin nhắn đến từ cuộc trò chuyện KHÔNG được chọn hiện tại
                boolean isCurrentRecipient = false;
                if (("chat".equals(m.type) || "gif".equals(m.type)) && "Public Chat".equals(viewModel.getCurrentRecipient())) {
                    isCurrentRecipient = true; // Tin nhắn public và đang xem Public Chat
                } else if (("dm".equals(m.type) || "dm_gif".equals(m.type)) && senderName.equals(viewModel.getCurrentRecipient())) {
                    isCurrentRecipient = true; // Tin nhắn DM và đang xem DM của người gửi này
                }

                // 2. Chỉ hiển thị thông báo nếu cửa sổ KHÔNG được tập trung HOẶC KHÔNG phải cuộc trò chuyện hiện tại
                if (!parentFrame.isFocused() || !isCurrentRecipient) {
                    JOptionPane.showMessageDialog(parentFrame, message, title, JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        // =======================================================
    }
}