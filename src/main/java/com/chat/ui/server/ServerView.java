package com.chat.ui.server;

import com.chat.service.DatabaseManager;
import com.chat.ui.server.action.BroadcastAction;
import com.chat.ui.server.action.KickAction;
import com.chat.ui.server.action.StartServerAction;
import com.chat.ui.server.action.StopServerAction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class ServerView extends JFrame {
    private final JTextField portField = new JTextField("5555");
    private final JButton startBtn = new JButton();
    private final JButton stopBtn = new JButton();
    private final JTextArea logArea = new JTextArea();
    private final DefaultListModel<String> clientListModel = new DefaultListModel<>();
    private final JList<String> clientList = new JList<>(clientListModel);
    private final JTextField broadcastField = new JTextField();
    private final JButton broadcastBtn = new JButton();
    private final JButton kickBtn = new JButton();

    // Đã loại bỏ 'final' để cho phép gán lại (injection)
    private ServerController controller;
    private final DatabaseManager dbManager = new DatabaseManager();

    public ServerView(ServerController controller) {
        super("Server");
        this.controller = controller;

        setupActions();
        layoutComponents();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        setRunningState(false);
    }

    // Phương thức mới để tiêm Controller (injection) và giải quyết dependency cycle
    public void setController(ServerController controller) {
        this.controller = controller;
        setupActions();
    }

    private void setupActions() {
        if (controller == null) return;

        startBtn.setAction(new StartServerAction(controller));
        stopBtn.setAction(new StopServerAction(controller));
        broadcastBtn.setAction(new BroadcastAction(controller, broadcastField));
        kickBtn.setAction(new KickAction(controller, clientList));
    }

    private void layoutComponents() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        // TOP Panel (Port & Control)
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.add(new JLabel("Port:"));
        portField.setColumns(6);
        top.add(portField);
        top.add(startBtn);
        top.add(stopBtn);
        root.add(top, BorderLayout.NORTH);

        // Center Split Pane (Log & Clients)
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.7);
        root.add(split, BorderLayout.CENTER);

        // Log Area
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        split.setLeftComponent(logScroll);

        // Right Panel (Client List & Kick Button)
        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.add(new JLabel("Connected Clients"), BorderLayout.NORTH);
        right.add(new JScrollPane(clientList), BorderLayout.CENTER);

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightBtns.add(kickBtn);
        right.add(rightBtns, BorderLayout.SOUTH);

        split.setRightComponent(right);

        // Bottom Panel (Broadcast)
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.add(broadcastField, BorderLayout.CENTER);
        bottom.add(broadcastBtn, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);
    }

    // Phương thức để Controller cập nhật UI
    public void appendLog(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(s);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void updateClientList(List<String> clientNames) {
        SwingUtilities.invokeLater(() -> {
            clientListModel.clear();
            for (String name : clientNames) clientListModel.addElement(name);
        });
    }

    public void setRunningState(boolean isRunning) {
        portField.setEnabled(!isRunning);
        startBtn.setEnabled(!isRunning);
        stopBtn.setEnabled(isRunning);
        broadcastBtn.setEnabled(isRunning);
        kickBtn.setEnabled(isRunning);
    }

    // Cung cấp Dependency cho Controller
    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }

    public int getPort() {
        try {
            return Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            return 5555;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf: " + ex);
        }
        SwingUtilities.invokeLater(() -> {
            // 1. Khởi tạo View với NULL
            ServerView view = new ServerView(null);
            // 2. Khởi tạo Controller (Controller cần View)
            ServerController controller = new ServerController(view, view.getPort());
            // 3. Tiêm Controller vào View
            view.setController(controller);
            view.setVisible(true);
        });
    }
}