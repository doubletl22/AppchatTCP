package com.chat.ui.client.action;

import com.chat.ui.client.ClientController;
import com.chat.ui.client.panel.ClientChatPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class SendAction extends AbstractAction {
    // FIX: Đổi từ private final sang public
    public ClientController controller;
    public ClientChatPanel chatPanel; // FIX: Đổi từ private final sang public

    public SendAction(ClientController controller, ClientChatPanel chatPanel) {
        super("Gửi");
        this.controller = controller;
        this.chatPanel = chatPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller == null || chatPanel == null) {
            // Lỗi hệ thống nếu chưa inject
            return;
        }

        String text = chatPanel.getInputText().trim();
        if (text.isEmpty()) return;

        chatPanel.clearInputField();
        controller.handleSend(text);
    }
}