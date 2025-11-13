package com.chat.ui.server;

import com.chat.core.ChatServerCore;
import com.chat.core.ServerLogListener;
import com.chat.model.Message;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class ServerController implements ServerLogListener {
    private final ServerView view;
    private ChatServerCore serverCore;
    private final int port;

    public ServerController(ServerView view, int port) {
        this.view = view;
        this.port = port;
    }

    // --- Actions/Command Handlers (Delegation) ---
    // CÁC PHƯƠNG THỨC NÀY BỊ THIẾU HOẶC BỊ LỖI TRONG PHIÊN BẢN TRƯỚC

    public void startServerAction(ActionEvent e) {
        if (serverCore != null && serverCore.isRunning()) return;
        try {
            // FIX: Sử dụng thứ tự tham số đúng: (port, DatabaseManager, ServerLogListener)
            serverCore = new ChatServerCore(port, view.getDatabaseManager(), this);
            serverCore.startServer();
            view.setRunningState(true);
        } catch (Exception ex) {
            // Lỗi 2: Gọi phương thức showErrorMessage
            showErrorMessage("Start server failed: " + ex.getMessage());
        }
    }

    public void stopServerAction(ActionEvent e) {
        if (serverCore == null || !serverCore.isRunning()) return;
        serverCore.stopServer();
        serverCore = null;
        view.setRunningState(false);
    }

    // Lỗi 3: Phương thức này cần cho BroadcastAction
    public void broadcastAction(String text) {
        if (text.isEmpty() || serverCore == null) return;
        serverCore.broadcast(Message.system("[SERVER]: " + text));
    }

    // Lỗi 4: Phương thức này cần cho KickAction
    public void kickSelectedAction(String selectedClient) {
        if (selectedClient == null || serverCore == null) return;
        serverCore.kickClient(selectedClient);
    }

    // --- ServerLogListener Implementation (UI Callbacks) ---
    // CÁC PHƯƠNG THỨC NÀY BẮT BUỘC PHẢI CÓ DO IMPLEMENT ServerLogListener

    @Override
    public void log(String s) {
        view.appendLog("[" + new java.text.SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + s + "\n");
    }

    @Override
    public void refreshClientList(List<String> clientNames) {
        view.updateClientList(clientNames);
    }

    // Lỗi 1: Phương thức này bị thiếu
    @Override
    public void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(view, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}