package com.chat.ui.client.action;

import com.chat.ui.client.ClientController;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class DisconnectAction extends AbstractAction {
    // FIX: Đổi từ private final sang public để cho phép injection từ View
    public ClientController controller;

    public DisconnectAction(ClientController controller) {
        super("Ngắt kết nối");
        this.controller = controller;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller != null) {
            controller.handleDisconnect();
        } else {
            // Có thể bỏ qua lỗi này nếu chắc chắn Controller được inject
        }
    }
}