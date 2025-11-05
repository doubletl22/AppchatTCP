package com.chat.ui.client.action;

import com.chat.ui.client.ClientController;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ConnectAction extends AbstractAction {
    // FIX: Đổi từ private final sang public
    public ClientController controller;

    public ConnectAction(ClientController controller) {
        super("Kết nối");
        this.controller = controller;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (controller != null) {
            controller.showLoginDialog();
        } else {
            JOptionPane.showMessageDialog(null, "Controller chưa được khởi tạo.", "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
        }
    }
}