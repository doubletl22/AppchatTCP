package com.chat.ui.server.action;

import com.chat.ui.server.ServerController;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class BroadcastAction extends AbstractAction {
    private final ServerController controller;
    private final JTextField broadcastField;

    public BroadcastAction(ServerController controller, JTextField broadcastField) {
        super("Broadcast");
        this.controller = controller;
        this.broadcastField = broadcastField;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String text = broadcastField.getText();
        if (text.isEmpty()) return;

        broadcastField.setText("");
        controller.broadcastAction(text);
    }
}