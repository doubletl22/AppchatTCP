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

    // [MỚI] Hàm xử lý gửi Voice
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
        // Bỏ qua tin nhắn do chính mình gửi (đã hiển thị Local Echo)
        if (m.name.equals(viewModel.getUserName())) return;
        if (m.name.startsWith("[TO ")) return; // Bỏ qua xác nhận DM

        viewModel.notifyMessageReceived(m);

        // Hiển thị thông báo Popup nếu cần
        UiUtils.invokeLater(() -> {
            String senderName = m.name != null ? m.name : "Hệ thống";
            boolean isVoice = "voice".equals(m.type) || "dm_voice".equals(m.type);

            if (isVoice && !parentFrame.isFocused()) {
                JOptionPane.showMessageDialog(parentFrame, "Bạn có tin nhắn thoại mới từ " + senderName, "Tin nhắn mới", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }
}