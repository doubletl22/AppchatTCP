package com.chat.ui.client.panel;

import com.chat.model.ClientViewModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

public class ClientConnectPanel extends JPanel {
    // Các thành phần nhập liệu
    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JTextField portField = new JTextField("5555");
    private final JLabel statusLabel = new JLabel("Trạng thái: Chưa kết nối");
    private final JButton connectBtn = new JButton("Kết nối");

    // Thành phần điều khiển hiển thị
    private final JButton toggleButton;
    private final JPanel containerPanel; // Panel chứa form nhập liệu (để ẩn/hiện)

    private final ClientViewModel viewModel;
    private final Action connectAction;
    private final Action disconnectAction;

    public ClientConnectPanel(ClientViewModel viewModel, Action connectAction, Action disconnectAction) {
        this.viewModel = viewModel;
        this.connectAction = connectAction;
        this.disconnectAction = disconnectAction;

        // Thiết lập layout chính
        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)));

        // --- PHẦN 1: HEADER (Luôn hiển thị) ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(8, 15, 8, 15));
        headerPanel.setBackground(UIManager.getColor("Panel.background"));

        // 1a. Trạng thái (Bên trái)
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 13f));
        statusLabel.setForeground(UIManager.getColor("Component.accentColor"));

        // 1b. Nút Toggle (Bên phải)
        toggleButton = new JButton("⚙ Cấu hình kết nối");
        toggleButton.setFont(toggleButton.getFont().deriveFont(12f));
        toggleButton.setBorderPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setFocusPainted(false);
        toggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleButton.setForeground(Color.GRAY);
        toggleButton.addActionListener(e -> toggleContainerPanel());

        headerPanel.add(statusLabel, BorderLayout.WEST);
        headerPanel.add(toggleButton, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // --- PHẦN 2: CONTAINER PANEL (Form nhập liệu) ---
        containerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        containerPanel.setBackground(UIManager.getColor("Panel.background"));
        containerPanel.setBorder(new EmptyBorder(0, 15, 10, 15));

        // Label và Input Host
        containerPanel.add(new JLabel("Host:"));
        hostField.setColumns(12);
        hostField.putClientProperty("Component.arc", 10);
        containerPanel.add(hostField);

        // Label và Input Port
        containerPanel.add(new JLabel("Port:"));
        portField.setColumns(6);
        portField.putClientProperty("Component.arc", 10);
        containerPanel.add(portField);

        // Nút Kết nối
        connectBtn.setAction(connectAction);
        connectBtn.setText("Kết nối");
        connectBtn.putClientProperty("JButton.buttonType", "roundRect");
        connectBtn.setPreferredSize(new Dimension(100, 30));
        containerPanel.add(connectBtn);

        add(containerPanel, BorderLayout.CENTER);
        containerPanel.setVisible(false);

        // --- [QUAN TRỌNG] LIÊN KẾT MODEL ĐÃ SỬA LỖI ---
        // Gộp chung vào 1 listener để đảm bảo cả 2 hàm đều được gọi
        viewModel.onStatusUpdate(status -> {
            setStatusLabel(status);
            updateButtonStates(status);
        });
    }

    private void toggleContainerPanel() {
        boolean isVisible = containerPanel.isVisible();
        containerPanel.setVisible(!isVisible);
        if (!isVisible) {
            toggleButton.setText("▲ Ẩn cấu hình");
            toggleButton.setForeground(UIManager.getColor("Component.accentColor"));
        } else {
            toggleButton.setText("⚙ Cấu hình kết nối");
            toggleButton.setForeground(Color.GRAY);
        }
        revalidate();
        repaint();
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

            if (containerPanel.isVisible()) {
                toggleContainerPanel();
            }
        } else {
            connectBtn.setAction(connectAction);
            connectBtn.setText("Kết nối");
            connectBtn.setBackground(UIManager.getColor("Component.accentColor"));
            connectBtn.setForeground(Color.WHITE);
            hostField.setEnabled(true);
            portField.setEnabled(true);
        }
    }

    public String getHost() { return hostField.getText(); }
    public String getPort() { return portField.getText(); }
}