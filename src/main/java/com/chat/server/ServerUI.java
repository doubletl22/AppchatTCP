package com.chat.server;

import com.chat.DatabaseManager;
import com.chat.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.List;

// Import ChatServerCore và ServerLogListener (Giả định nằm trong cùng package)

public class ServerUI extends JFrame implements ServerLogListener {

    private final JTextField portField = new JTextField("5555");
    private final JButton startBtn = new JButton("Start");
    private final JButton stopBtn = new JButton("Stop");
    private final JTextArea logArea = new JTextArea();
    private final DefaultListModel<String> clientListModel = new DefaultListModel<>();
    private final JList<String> clientList = new JList<>(clientListModel);
    private final JTextField broadcastField = new JTextField();
    private final JButton broadcastBtn = new JButton("Broadcast");
    private final JButton kickBtn = new JButton("Kick Selected");

    // Core/Service Layer Instance
    private ChatServerCore serverCore;
    private final DatabaseManager dbManager = new DatabaseManager();

    // Constructor (UI setup) remains largely the same...
    public ServerUI() {
        super("TCP Chat Server (Swing)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // ... (UI setup code)

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.add(new JLabel("Port:"));
        portField.setColumns(6);
        top.add(portField);
        startBtn.addActionListener(this::startServer);
        stopBtn.addActionListener(this::stopServer);
        stopBtn.setEnabled(false);
        top.add(startBtn);
        top.add(stopBtn);
        root.add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.7);
        root.add(split, BorderLayout.CENTER);

        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        split.setLeftComponent(logScroll);

        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.add(new JLabel("Connected Clients"), BorderLayout.NORTH);
        right.add(new JScrollPane(clientList), BorderLayout.CENTER);

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        kickBtn.addActionListener(e -> kickSelected());
        kickBtn.setEnabled(false);
        rightBtns.add(kickBtn);
        right.add(rightBtns, BorderLayout.SOUTH);

        split.setRightComponent(right);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.add(broadcastField, BorderLayout.CENTER);
        broadcastBtn.addActionListener(e -> broadcastFromServer());
        broadcastBtn.setEnabled(false);
        bottom.add(broadcastBtn, BorderLayout.EAST);
        root.add(bottom, BorderLayout.SOUTH);
    }

    // Implementation of ServerLogListener (Callbacks from Core to UI)
    @Override
    public void log(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + new java.text.SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + s + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    @Override
    public void refreshClientList(List<String> clientNames) {
        SwingUtilities.invokeLater(() -> {
            clientListModel.clear();
            for (String name : clientNames) clientListModel.addElement(name);
        });
    }

    @Override
    public void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // Delegates control to the Core Layer
    private void startServer(ActionEvent e) {
        if (serverCore != null && serverCore.isRunning()) return;
        try {
            int port = Integer.parseInt(portField.getText().trim());

            // Initialize and configure the Core Server Layer
            serverCore = new ChatServerCore(port, dbManager, this);
            serverCore.startServer();

            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            broadcastBtn.setEnabled(true);
            kickBtn.setEnabled(true);
        } catch (NumberFormatException ex) {
            showErrorMessage("Port must be a valid integer.");
        } catch (Exception ex) {
            showErrorMessage("Start server failed: " + ex.getMessage());
        }
    }

    // Delegates control to the Core Layer
    private void stopServer(ActionEvent e) {
        if (serverCore == null || !serverCore.isRunning()) return;
        serverCore.stopServer();
        serverCore = null;

        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        broadcastBtn.setEnabled(false);
        kickBtn.setEnabled(false);
    }

    // Delegates a broadcast request to the Core Layer
    private void broadcastFromServer() {
        String text = broadcastField.getText().trim();
        if (text.isEmpty() || serverCore == null) return;
        broadcastField.setText("");
        serverCore.broadcast(Message.system("[SERVER]: " + text));
    }

    // Delegates a kick request to the Core Layer
    private void kickSelected() {
        String selected = clientList.getSelectedValue();
        if (selected == null || serverCore == null) return;
        serverCore.kickClient(selected);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerUI().setVisible(true));
    }
}