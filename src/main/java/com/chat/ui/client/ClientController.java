package com.chat.ui.client;

import com.chat.core.ChatClientCore;
import com.chat.core.ClientStatusListener;
import com.chat.model.ClientViewModel;
import com.chat.model.Message;
import com.chat.ui.client.dialog.LoginDialog;
import com.chat.util.UiUtils;

import javax.swing.*;
import java.awt.*;
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

    // --- LOGIC KẾT NỐI & LOGIN ---
    public void showLoginDialog() {
        boolean isRegisterMode = false;
        while (true) {
            LoginDialog dialog = new LoginDialog(parentFrame, isRegisterMode);
            dialog.setVisible(true);

            if (dialog.isCancelled()) return;

            String action = dialog.getAction();
            if ("switch".equals(action)) {
                isRegisterMode = !isRegisterMode;
                continue;
            }

            String host = dialog.getHost();
            int port = dialog.getPort();
            String username = dialog.getUsername();
            String password = dialog.getPassword();

            handleConnect(host, port, username, password, action);
            break;
        }
    }

    public void handleConnect(String host, int port, String username, String password, String action) {
        if (clientCore.isConnected()) clientCore.disconnect("Reconnect");

        viewModel.setConnectionStatus(true, false);
        viewModel.notifyMessageReceived(Message.system("Đang kết nối tới " + host + ":" + port + "..."));

        try {
            clientCore.connectAndAuth(host, port, username, password, action);
        } catch (IOException ex) {
            viewModel.notifyMessageReceived(Message.system("[LỖI] Không thể kết nối: " + ex.getMessage()));
            viewModel.setConnectionStatus(false, false);
            JOptionPane.showMessageDialog(parentFrame, "Không thể kết nối đến Server!\n" + ex.getMessage(), "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void handleDisconnect() {
        clientCore.disconnect("Người dùng ngắt kết nối.");
    }

    // --- XỬ LÝ KẾT QUẢ TỪ SERVER ---
    @Override
    public void onConnectSuccess(String userName) {
        viewModel.setUserName(userName);
        viewModel.setConnectionStatus(true, true);
        viewModel.notifyMessageReceived(Message.system("Đăng nhập thành công! Xin chào " + userName));

        // TEST NGAY LẬP TỨC KHI ĐĂNG NHẬP
        showNotification(Message.system("Xin chào " + userName + "! Hệ thống thông báo đã hoạt động."));
    }

    @Override
    public void onAuthFailure(String reason) {
        if (reason != null && reason.toLowerCase().contains("thành công")) {
            UiUtils.invokeLater(() -> {
                JOptionPane.showMessageDialog(parentFrame, "Đăng ký THÀNH CÔNG! Hãy đăng nhập.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                showLoginDialog();
            });
        } else {
            UiUtils.invokeLater(() -> JOptionPane.showMessageDialog(parentFrame, "Thất bại: " + reason, "Lỗi", JOptionPane.ERROR_MESSAGE));
            viewModel.notifyMessageReceived(Message.system("[THẤT BẠI] " + reason));
        }
        handleDisconnect();
    }

    // --- CÁC HÀM GỬI TIN NHẮN ---
    public void handleSend(String text) {
        if (!clientCore.isAuthenticated()) return;
        String recipient = viewModel.getCurrentRecipient();
        try {
            if (text.trim().toLowerCase().startsWith("/gif ")) {
                clientCore.sendGif(text.trim().substring(5).trim(), recipient);
            } else {
                clientCore.sendMessage(text, recipient);
                Message m = "Public Chat".equals(recipient) ? Message.chat(viewModel.getUserName(), text) : Message.dm(viewModel.getUserName(), recipient, text, true);
                viewModel.notifyMessageReceived(m);
            }
        } catch (IOException ex) {
            viewModel.notifyMessageReceived(Message.system("[LỖI] " + ex.getMessage()));
            handleDisconnect();
        }
    }

    public void handleSendVoice(String base64Data) {
        if (!clientCore.isAuthenticated()) return;
        try {
            clientCore.sendVoice(base64Data, viewModel.getCurrentRecipient());
            Message m = Message.voice(base64Data, viewModel.getCurrentRecipient());
            m.name = viewModel.getUserName(); m.isSelf = true;
            if (!"Public Chat".equals(viewModel.getCurrentRecipient())) m.type = "dm_voice";
            viewModel.notifyMessageReceived(m);
        } catch (IOException ex) { viewModel.notifyMessageReceived(Message.system("[LỖI Voice] " + ex.getMessage())); }
    }

    public void handleSendImage(File imageFile) {
        if (!clientCore.isAuthenticated()) return;
        new Thread(() -> {
            try {
                String base64 = com.chat.util.ImageUtils.encodeImageToBase64(imageFile);
                if (base64 == null) return;
                clientCore.sendImage(base64, viewModel.getCurrentRecipient());
                UiUtils.invokeLater(() -> {
                    Message m = Message.image(base64, viewModel.getCurrentRecipient());
                    m.name = viewModel.getUserName(); m.isSelf = true;
                    if (!"Public Chat".equals(viewModel.getCurrentRecipient())) m.type = "dm_image";
                    viewModel.notifyMessageReceived(m);
                });
            } catch (Exception ex) { viewModel.notifyMessageReceived(Message.system("[LỖI Ảnh] " + ex.getMessage())); }
        }).start();
    }

    public void handleSendSticker(String stickerPath) {
        if (!clientCore.isAuthenticated()) return;
        String recipient = viewModel.getCurrentRecipient();
        try {
            clientCore.sendMessage("[STICKER]: " + stickerPath, recipient);
            UiUtils.invokeLater(() -> {
                Message m = Message.sticker(stickerPath, recipient);
                m.name = viewModel.getUserName(); m.isSelf = true;
                if (!"Public Chat".equals(recipient)) m.type = "dm_sticker";
                viewModel.notifyMessageReceived(m);
            });
        } catch (IOException ex) {
            viewModel.notifyMessageReceived(Message.system("[LỖI Sticker] " + ex.getMessage()));
        }
    }

    public void requestHistory(String targetUser) {
        try {
            if ("Public Chat".equals(targetUser)) clientCore.requestPublicHistory();
            else clientCore.requestDirectHistory(targetUser);
        } catch (IOException ex) { viewModel.notifyMessageReceived(Message.system("[LỖI] " + ex.getMessage())); }
    }

    @Override
    public void onDisconnect(String reason) {
        viewModel.setConnectionStatus(false, false);
        viewModel.updateUsers(Collections.emptyList());
        viewModel.setCurrentRecipient("Public Chat");
        viewModel.notifyMessageReceived(Message.system("Đã ngắt kết nối: " + reason));
    }

    @Override
    public void onSystemMessage(String text) {
        viewModel.notifyMessageReceived(Message.system(text));
        if (text.endsWith(" joined the chat.")) viewModel.addUser(text.substring(0, text.indexOf(" joined")));
        else if (text.endsWith(" left the chat.")) viewModel.removeUser(text.substring(0, text.indexOf(" left")));
    }

    @Override
    public void onUserListUpdate(List<String> userNames, String selfName) {
        viewModel.updateUsers(userNames);
    }

    // --- XỬ LÝ NHẬN TIN NHẮN (QUAN TRỌNG) ---
    @Override
    public void onMessageReceived(Message m) {
        System.out.println("DEBUG: Nhận được tin nhắn từ Server: " + m.type + " | " + m.text);

        boolean isHistory = m.type != null && m.type.contains("history");

        // 1. Nếu là tin nhắn lịch sử -> CHỈ hiện lên chat, KHÔNG thông báo
        if (isHistory) {
            viewModel.notifyMessageReceived(m);
            return;
        }

        // 2. Nếu là tin nhắn của chính mình (do Local Echo hoặc Server gửi lại) -> Bỏ qua
        if (m.name != null && m.name.equals(viewModel.getUserName())) {
            return;
        }

        // 3. Xử lý Sticker (tương tự code cũ)
        if (m.text != null && m.text.startsWith("[STICKER]:")) {
            m.text = m.text.substring(10).trim();
            m.type = (m.type != null && m.type.startsWith("dm")) ? "dm_sticker" : "sticker";
        }

        // 4. HIỂN THỊ THÔNG BÁO
        // Đã xóa điều kiện isFocused() -> Luôn hiện để test
        showNotification(m);

        // 5. Cập nhật giao diện chat
        viewModel.notifyMessageReceived(m);
    }

    private void showNotification(Message msg) {
        // Tạo nội dung thông báo
        String content;
        if ("chat".equals(msg.type)) content = "<b>" + msg.name + "</b>: " + msg.text;
        else if ("dm".equals(msg.type)) content = "<span style='color:yellow'>[Mật]</span> <b>" + msg.name + "</b>: " + msg.text;
        else if (msg.type != null && msg.type.contains("image")) content = "<b>" + msg.name + "</b> đã gửi 1 ảnh 📷";
        else if (msg.type != null && msg.type.contains("sticker")) content = "<b>" + msg.name + "</b> đã gửi 1 sticker 😊";
        else if (msg.type != null && msg.type.contains("voice")) content = "<b>" + msg.name + "</b> đã gửi voice 🎤";
        else content = msg.text;

        // Âm thanh báo hiệu
        Toolkit.getDefaultToolkit().beep();

        // Gọi Toast hiện lên
        Toast.show(parentFrame, content);
    }
}