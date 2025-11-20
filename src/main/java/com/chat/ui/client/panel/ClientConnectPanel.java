package com.chat.ui.client.panel;

import com.chat.model.ClientViewModel;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

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

        // Thiết lập layout chính cho toàn bộ ClientConnectPanel
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 5, 5, 5));

        // --- PHẦN 1: NÚT TOGGLE (Luôn hiển thị ở trên cùng) ---
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toggleButton = new JButton("▼ Cấu hình kết nối");
        toggleButton.setBorderPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setFocusPainted(false);
        toggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // Sự kiện: Bấm nút thì gọi hàm toggle
        toggleButton.addActionListener(e -> toggleContainerPanel());

        headerPanel.add(toggleButton);
        add(headerPanel, BorderLayout.NORTH);

        // --- PHẦN 2: CONTAINER PANEL (Chứa form, sẽ bị ẩn/hiện) ---
        containerPanel = new JPanel(new BorderLayout());
        containerPanel.setBorder(new EmptyBorder(5, 10, 5, 10));

        // 2a. Status (Bên trái)
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);
        containerPanel.add(statusPanel, BorderLayout.WEST);

        // 2b. Controls (Bên phải: Host, Port, Button)
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        controlsPanel.add(new JLabel("Host:"));
        hostField.setColumns(10);
        controlsPanel.add(hostField); // Đã thêm Host field vào giao diện

        controlsPanel.add(new JLabel("Port:"));
        portField.setColumns(4);
        controlsPanel.add(portField);

        // Cài đặt Action cho nút Connect
        connectBtn.setAction(connectAction);
        connectBtn.setText("Kết nối"); // Set text lại vì Action có thể ghi đè
        controlsPanel.add(connectBtn);

        containerPanel.add(controlsPanel, BorderLayout.EAST);

        // Thêm containerPanel vào giữa
        add(containerPanel, BorderLayout.CENTER);

        // Mặc định ẩn form đi lúc khởi tạo
        containerPanel.setVisible(false);

        // --- LIÊN KẾT MODEL ---
        viewModel.onStatusUpdate(this::setStatusLabel);
        viewModel.onStatusUpdate(this::updateButtonStates);
    }

    // Logic ẩn hiện form
    private void toggleContainerPanel() {
        boolean isVisible = containerPanel.isVisible();

        // Đảo ngược trạng thái
        containerPanel.setVisible(!isVisible);

        // Đổi text nút bấm
        if (!isVisible) {
            toggleButton.setText("▲ Ẩn cấu hình");
        } else {
            toggleButton.setText("▼ Cấu hình kết nối");
        }

        // Cập nhật giao diện ngay lập tức
        revalidate();
        repaint();
    }

    public void setStatusLabel(String status) {
        statusLabel.setText("Trạng thái: " + status);
    }

    private void updateButtonStates(String status) {
        boolean connected = viewModel.isConnected();
        connectBtn.setEnabled(true);

        if (connected) {
            connectBtn.setAction(disconnectAction);
            connectBtn.setText("Ngắt kết nối");
            hostField.setEnabled(false);
            portField.setEnabled(false);
            connectBtn.putClientProperty("JButton.buttonType", "danger");
        } else {
            connectBtn.setAction(connectAction);
            connectBtn.setText("Kết nối");
            hostField.setEnabled(true);
            portField.setEnabled(true);
            connectBtn.putClientProperty("JButton.buttonType", "default");
        }
    }

    public String getHost() { return hostField.getText(); }
    public String getPort() { return portField.getText(); }
}