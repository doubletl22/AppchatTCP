package com.chat.ui.client;

import com.chat.core.ChatClientCore;
import com.chat.core.ClientStatusListener;
import com.chat.model.ClientViewModel;
import com.chat.model.Message;
import com.chat.ui.client.dialog.LoginDialog;
import com.chat.util.UiUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ClientController implements ClientStatusListener {

    private final ChatClientCore clientCore;
    private final ClientViewModel viewModel;
    private final JFrame parentFrame;

    public ClientController(JFrame parentFrame, ChatClientCore clientCore, ClientViewModel viewModel) {
        this.parentFrame = parentFrame;
        this.clientCore = clientCore;
        this.viewModel = viewModel;
    }

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
        boolean isGif = text.trim().toLowerCase().startsWith("/gif ");

        try {
            if (isGif) {
                String gifText = text.trim().substring(5).trim();
                if (gifText.isEmpty()) return;
                clientCore.sendGif(gifText, recipient);
                if ("Public Chat".equals(recipient)) {
                    viewModel.notifyMessageReceived(Message.chat(userName, "[GIF]: " + gifText));
                } else {
                    viewModel.notifyMessageReceived(Message.dm(userName, recipient, "[GIF]: " + gifText, true));
                }
            } else {
                clientCore.sendMessage(text, recipient);
                if ("Public Chat".equals(recipient)) {
                    viewModel.notifyMessageReceived(Message.chat(userName, text));
                } else {
                    viewModel.notifyMessageReceived(Message.dm(userName, recipient, text, true));
                }
            }
        } catch (IOException ex) {
            viewModel.notifyMessageReceived(Message.system("[ERROR] " + ex.getMessage()));
            handleDisconnect();
        }
    }

    // Hàm xử lý gửi Voice
    public void handleSendVoice(String base64Data) {
        if (!clientCore.isAuthenticated()) return;
        String recipient = viewModel.getCurrentRecipient();
        String userName = viewModel.getUserName();

        try {
            clientCore.sendVoice(base64Data, recipient);

            // Hiển thị ngay tin nhắn thoại của chính mình (Local Echo)
            if ("Public Chat".equals(recipient)) {
                // Tự tạo tin nhắn để hiển thị
                Message m = Message.voice(base64Data, "Public Chat");
                m.name = userName;
                viewModel.notifyMessageReceived(m);
            } else {
                Message m = Message.voice(base64Data, recipient); // recipient here is target
                m.name = userName;
                m.type = "dm_voice"; // Đánh dấu là DM
                m.isSelf = true;
                viewModel.notifyMessageReceived(m);
            }

        } catch (IOException ex) {
            viewModel.notifyMessageReceived(Message.system("[ERROR Gửi Voice] " + ex.getMessage()));
        }
    }

    // Hàm xử lý gửi Ảnh từ file
    public void handleSendImage(File imageFile) {
        if (!clientCore.isAuthenticated()) return;
        String recipient = viewModel.getCurrentRecipient();
        String userName = viewModel.getUserName();

        // Chạy luồng riêng để nén ảnh không làm đơ giao diện
        new Thread(() -> {
            try {
                // Sử dụng ImageUtils để nén và chuyển đổi sang Base64
                String base64 = com.chat.util.ImageUtils.encodeImageToBase64(imageFile);
                if (base64 == null) return;

                // Gửi qua mạng
                clientCore.sendImage(base64, recipient);

                // Local Echo (Hiển thị ngay lập tức trên máy mình)
                UiUtils.invokeLater(() -> {
                    Message m = Message.image(base64, recipient);
                    m.name = userName;
                    m.isSelf = true;
                    // Nếu là DM, cần chỉnh lại type để hiển thị đúng bong bóng chat
                    if (!"Public Chat".equals(recipient)) {
                        m.type = "dm_image";
                    }
                    viewModel.notifyMessageReceived(m);
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                viewModel.notifyMessageReceived(Message.system("[ERROR Gửi Ảnh] " + ex.getMessage()));
            }
        }).start();
    }

    // [MỚI] Hàm xử lý gửi Sticker
    public void handleSendSticker(String stickerPath) {
        if (!clientCore.isAuthenticated()) return;
        String recipient = viewModel.getCurrentRecipient();
        String userName = viewModel.getUserName();

        try {
            // 1. Gửi qua mạng
            clientCore.sendSticker(stickerPath, recipient);

            // 2. Hiển thị ngay lập tức trên máy mình (Local Echo)
            if ("Public Chat".equals(recipient)) {
                // Tự tạo tin nhắn giả để hiển thị
                Message m = Message.sticker(stickerPath, "Public Chat");
                m.name = userName;
                m.isSelf = true; // Đánh dấu là tin nhắn của mình
                viewModel.notifyMessageReceived(m);
            } else {
                Message m = Message.sticker(stickerPath, recipient);
                m.name = userName;
                m.type = "dm_sticker"; // Đánh dấu là DM
                m.isSelf = true;
                viewModel.notifyMessageReceived(m);
            }

        } catch (IOException ex) {
            viewModel.notifyMessageReceived(Message.system("[ERROR Gửi Sticker] " + ex.getMessage()));
        }
    }

    // Yêu cầu lịch sử DM (Chat riêng)
    public void requestHistory(String targetUser) {
        try {
            clientCore.requestDirectHistory(targetUser);
            viewModel.notifyMessageReceived(Message.system("Đang tải lịch sử tin nhắn..."));
        } catch (IOException ex) {
            viewModel.notifyMessageReceived(Message.system("[ERROR] Không thể yêu cầu lịch sử DM: " + ex.getMessage()));
        }
    }

    // [MỚI] Yêu cầu lịch sử Chat Chung (Public Chat)
    public void requestPublicChatHistory() {
        try {
            clientCore.requestPublicHistory();
            viewModel.notifyMessageReceived(Message.system("Đang tải lại lịch sử chat chung..."));
        } catch (IOException ex) {
            viewModel.notifyMessageReceived(Message.system("[Lỗi] Không thể tải lịch sử: " + ex.getMessage()));
        }
    }

    public void showLoginDialog() {
        LoginDialog dialog = new LoginDialog(parentFrame);
        dialog.setVisible(true);
        if (dialog.isCancelled()) return;
        String host = dialog.getHost().trim();
        int port = dialog.getPort();
        final String username = dialog.getUsername().trim();
        final String password = dialog.getPassword().trim();
        final String action = dialog.getAction();

        if (username.isEmpty() || password.isEmpty()) return;
        if (port == -1) return;

        handleConnect(host, port, username, password, action);
    }

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
        viewModel.notifyMessageReceived(Message.system("Disconnected. Reason: " + reason));
    }

    @Override
    public void onAuthFailure(String reason) {
        viewModel.notifyMessageReceived(Message.system("[AUTH FAILED] " + reason));
        handleDisconnect();
    }

    @Override
    public void onSystemMessage(String text) {
        viewModel.notifyMessageReceived(Message.system(text));
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
        // [CẬP NHẬT] Logic lọc tin nhắn để KHÔNG ẩn tin nhắn lịch sử của chính mình

        // 1. Kiểm tra xem đây có phải là tin nhắn Lịch sử không?
        boolean isHistory = "history".equals(m.type) || "dm_history".equals(m.type) ||
                "sticker_history".equals(m.type) || "dm_sticker_history".equals(m.type) ||
                "gif_history".equals(m.type) || "dm_gif_history".equals(m.type);

        // 2. Nếu KHÔNG PHẢI lịch sử, mà là tin nhắn của chính mình
        // -> Bỏ qua (vì đã hiển thị Local Echo lúc gửi rồi)
        // -> Logic này giúp tránh hiện 2 tin nhắn khi vừa gửi xong
        if (!isHistory && m.name != null && m.name.equals(viewModel.getUserName())) {
            return;
        }

        // 3. Bỏ qua tin nhắn xác nhận [TO ...] (Do server gửi về xác nhận đã gửi DM thành công)
        if (m.name != null && m.name.startsWith("[TO ")) return;

        viewModel.notifyMessageReceived(m);

        // Hiển thị thông báo Popup nếu cần (chỉ khi app không focus)
        UiUtils.invokeLater(() -> {
            // Không thông báo nếu là tin lịch sử hoặc tin hệ thống
            if (isHistory || "system".equals(m.type)) return;

            String senderName = m.name != null ? m.name : "Hệ thống";
            boolean isVoice = "voice".equals(m.type) || "dm_voice".equals(m.type);
            boolean isImage = "image".equals(m.type) || "dm_image".equals(m.type);
            boolean isSticker = "sticker".equals(m.type) || "dm_sticker".equals(m.type);

            // [ĐÃ SỬA] Thêm kiểm tra cho Text và GIF
            boolean isGif = "gif".equals(m.type) || "dm_gif".equals(m.type);
            boolean isText = "chat".equals(m.type) || "dm".equals(m.type);

            // Cập nhật điều kiện hiển thị
            if ((isVoice || isImage || isSticker || isGif || isText) && !parentFrame.isFocused()) {
                String msgType = "tin nhắn mới"; // Mặc định cho Text

                if (isVoice) msgType = "tin nhắn thoại";
                else if (isImage) msgType = "một hình ảnh";
                else if (isSticker) msgType = "một sticker";
                else if (isGif) msgType = "một hình động (GIF)";

                JOptionPane.showMessageDialog(parentFrame, "Bạn có " + msgType + " mới từ " + senderName, "Tin nhắn mới", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }
}