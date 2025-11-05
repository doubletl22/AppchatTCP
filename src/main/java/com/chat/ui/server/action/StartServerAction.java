package com.chat.ui.server.action;

import com.chat.ui.server.ServerController;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class StartServerAction extends AbstractAction {
    private final ServerController controller;

    public StartServerAction(ServerController controller) {
        super("Start");
        this.controller = controller;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        controller.startServerAction(e);
    }
}