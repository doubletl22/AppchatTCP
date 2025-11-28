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

    // --- LOGIC KẾT NỐI & LOGIN ---
    public void showLoginDialog() {
        boolean isRegisterMode = false; // Mặc định hiện bảng Đăng nhập trước

        while (true) {
            // Mở Dialog với chế độ hiện tại
            LoginDialog dialog = new LoginDialog(parentFrame, isRegisterMode);
            dialog.setVisible(true);

            // Nếu người dùng tắt bảng (bấm X) -> Thoát luôn
            if (dialog.isCancelled()) return;

            String action = dialog.getAction();

            // Nếu người dùng bấm nút chuyển đổi (Đăng ký <-> Đăng nhập)
            if ("switch".equals(action)) {
                isRegisterMode = !isRegisterMode; // Đảo ngược chế độ
                continue; // Lặp lại vòng while để hiện bảng mới
            }

            // Nếu là hành động Login hoặc Register thật -> Thực hiện kết nối
            String host = dialog.getHost();
            int port = dialog.getPort();
            String username = dialog.getUsername();
            String password = dialog.getPassword();

            handleConnect(host, port, username, password, action);
            break; // Thoát vòng lặp
        }
    }

    public void handleConnect(String host, int port, String username, String password, String action) {
        if (clientCore.isConnected()) clientCore.disconnect("Reconnect"); // Ngắt cũ nếu có

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
    }

    @Override
    public void onAuthFailure(String reason) {
        // [QUAN TRỌNG] Kiểm tra xem có phải tin nhắn ĐĂNG KÝ THÀNH CÔNG không
        // Server gửi về dạng: "Đăng ký thành công. Hãy đăng nhập." nhưng flag là authFailure để ngắt kết nối

        if (reason != null && reason.toLowerCase().contains("thành công")) {
            UiUtils.invokeLater(() -> {
                JOptionPane.showMessageDialog(parentFrame,
                        "Đăng ký tài khoản THÀNH CÔNG!\nBạn có thể đăng nhập ngay bây giờ.",
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE);
                // Sau khi bấm OK, tự động mở lại dialog đăng nhập để tiện lợi
                showLoginDialog();
            });
        } else {
            // Đây là lỗi thật (Sai pass, trùng tên...)
            UiUtils.invokeLater(() -> {
                JOptionPane.showMessageDialog(parentFrame,
                        "Thao tác thất bại: " + reason,
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            });
            viewModel.notifyMessageReceived(Message.system("[THẤT BẠI] " + reason));
        }

        handleDisconnect();
    }

    // --- CÁC HÀM CHỨC NĂNG KHÁC (Giữ nguyên logic cũ của bạn) ---
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
                if ("Public Chat".equals(recipient)) viewModel.notifyMessageReceived(Message.chat(userName, "[GIF]: " + gifText));
                else viewModel.notifyMessageReceived(Message.dm(userName, recipient, "[GIF]: " + gifText, true));
            } else {
                clientCore.sendMessage(text, recipient);
                if ("Public Chat".equals(recipient)) viewModel.notifyMessageReceived(Message.chat(userName, text));
                else viewModel.notifyMessageReceived(Message.dm(userName, recipient, text, true));
            }
        } catch (IOException ex) {
            viewModel.notifyMessageReceived(Message.system("[LỖI] " + ex.getMessage()));
            handleDisconnect();
        }
    }

    public void handleSendVoice(String base64Data) {
        if (!clientCore.isAuthenticated()) return;
        String recipient = viewModel.getCurrentRecipient();
        try {
            clientCore.sendVoice(base64Data, recipient);
            Message m = Message.voice(base64Data, recipient);
            m.name = viewModel.getUserName();
            m.isSelf = true;
            if (!"Public Chat".equals(recipient)) m.type = "dm_voice";
            viewModel.notifyMessageReceived(m);
        } catch (IOException ex) { viewModel.notifyMessageReceived(Message.system("[LỖI Voice] " + ex.getMessage())); }
    }

    public void handleSendImage(File imageFile) {
        if (!clientCore.isAuthenticated()) return;
        String recipient = viewModel.getCurrentRecipient();
        new Thread(() -> {
            try {
                String base64 = com.chat.util.ImageUtils.encodeImageToBase64(imageFile);
                if (base64 == null) return;
                clientCore.sendImage(base64, recipient);
                UiUtils.invokeLater(() -> {
                    Message m = Message.image(base64, recipient);
                    m.name = viewModel.getUserName();
                    m.isSelf = true;
                    if (!"Public Chat".equals(recipient)) m.type = "dm_image";
                    viewModel.notifyMessageReceived(m);
                });
            } catch (Exception ex) { viewModel.notifyMessageReceived(Message.system("[LỖI Ảnh] " + ex.getMessage())); }
        }).start();
    }

    public void requestHistory(String targetUser) {
        try { clientCore.requestDirectHistory(targetUser); }
        catch (IOException ex) { viewModel.notifyMessageReceived(Message.system("[LỖI] " + ex.getMessage())); }
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

    @Override
    public void onMessageReceived(Message m) {
        if (m.name.equals(viewModel.getUserName()) || m.name.startsWith("[TO ")) return;
        viewModel.notifyMessageReceived(m);
    }
}

