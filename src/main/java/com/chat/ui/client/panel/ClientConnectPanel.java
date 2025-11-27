package com.chat.ui.client.panel;

import com.chat.model.ClientViewModel;
import com.chat.util.UiUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
// Đã xóa dòng import sai: java.awt.event.Action

public class ClientConnectPanel extends JPanel {
    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JTextField portField = new JTextField("5555");
    private final JLabel statusLabel = new JLabel("Trạng thái: Chưa kết nối");
    private final JButton connectBtn = new JButton("Kết nối");

    private final JButton toggleButton;
    private final JButton themeBtn; // Nút theme mới
    private final JPanel containerPanel;

    private final ClientViewModel viewModel;
    private final Action connectAction; // javax.swing.Action
    private final Action disconnectAction;

    public ClientConnectPanel(ClientViewModel viewModel, Action connectAction, Action disconnectAction) {
        this.viewModel = viewModel;
        this.connectAction = connectAction;
        this.disconnectAction = disconnectAction;

        setLayout(new BorderLayout());
        // Để màu nền tự động theo theme
        // setBackground(UIManager.getColor("Panel.background"));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)));

        // --- HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(8, 15, 8, 15));
        headerPanel.setOpaque(false);

        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 13f));
        statusLabel.setForeground(UiUtils.TEAL_COLOR);

        // Panel chứa các nút bên phải
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightHeader.setOpaque(false);

        // 1. Nút Đổi Theme
        themeBtn = new JButton("🌗");
        styleHeaderButton(themeBtn);
        themeBtn.setToolTipText("Chuyển chế độ Sáng/Tối");
        themeBtn.addActionListener(e -> UiUtils.toggleTheme());

        // 2. Nút Cấu hình
        toggleButton = new JButton("⚙ Cấu hình");
        styleHeaderButton(toggleButton);
        toggleButton.addActionListener(e -> toggleContainerPanel());

        rightHeader.add(themeBtn);
        rightHeader.add(toggleButton);

        headerPanel.add(statusLabel, BorderLayout.WEST);
        headerPanel.add(rightHeader, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // --- FORM NHẬP LIỆU ---
        containerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        containerPanel.setOpaque(false);
        containerPanel.setBorder(new EmptyBorder(0, 15, 10, 15));

        containerPanel.add(new JLabel("Host:"));
        hostField.setColumns(12);
        containerPanel.add(hostField);

        containerPanel.add(new JLabel("Port:"));
        portField.setColumns(6);
        containerPanel.add(portField);

        connectBtn.setAction(connectAction);
        connectBtn.setText("Kết nối");
        connectBtn.putClientProperty("JButton.buttonType", "roundRect");
        connectBtn.setPreferredSize(new Dimension(100, 30));
        containerPanel.add(connectBtn);

        add(containerPanel, BorderLayout.CENTER);
        containerPanel.setVisible(false);

        viewModel.onStatusUpdate(status -> {
            setStatusLabel(status);
            updateButtonStates(status);
        });
    }

    private void styleHeaderButton(JButton btn) {
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setForeground(Color.GRAY);
        btn.setFont(btn.getFont().deriveFont(14f));
    }

    private void toggleContainerPanel() {
        boolean isVisible = containerPanel.isVisible();
        containerPanel.setVisible(!isVisible);
        toggleButton.setForeground(isVisible ? Color.GRAY : UiUtils.TEAL_COLOR);
    }

    public void setStatusLabel(String status) {
        if (status.startsWith("Tên người dùng:")) {
            statusLabel.setText("👤 " + status);
        } else {
            statusLabel.setText("Trạng thái: " + status);
        }
    }

    private void updateButtonStates(String status) {
        boolean connected = viewModel.isConnected();
        connectBtn.setEnabled(true);

        if (connected) {
            connectBtn.setAction(disconnectAction);
            connectBtn.setText("Ngắt kết nối");
            connectBtn.setBackground(new Color(220, 53, 69));
            connectBtn.setForeground(Color.WHITE);
            hostField.setEnabled(false);
            portField.setEnabled(false);
            if (containerPanel.isVisible()) toggleContainerPanel();
        } else {
            connectBtn.setAction(connectAction);
            connectBtn.setText("Kết nối");
            connectBtn.setBackground(UiUtils.TEAL_COLOR);
            connectBtn.setForeground(Color.WHITE);
            hostField.setEnabled(true);
            portField.setEnabled(true);
        }
    }

    public String getHost() { return hostField.getText(); }
    public String getPort() { return portField.getText(); }
}