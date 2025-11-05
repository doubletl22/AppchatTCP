package com.chat.ui.server.action;

import com.chat.ui.server.ServerController;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class KickAction extends AbstractAction {
    private final ServerController controller;
    private final JList<String> clientList;

    public KickAction(ServerController controller, JList<String> clientList) {
        super("Kick Selected");
        this.controller = controller;
        this.clientList = clientList;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String selected = clientList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(clientList, "Vui lòng chọn một client để kick.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        controller.kickSelectedAction(selected);
    }
}