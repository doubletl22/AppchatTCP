package com.chat.ui.server.action;

import com.chat.ui.server.ServerController;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class StopServerAction extends AbstractAction {
    private final ServerController controller;

    public StopServerAction(ServerController controller) {
        super("Stop");
        this.controller = controller;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        controller.stopServerAction(e);
    }
}