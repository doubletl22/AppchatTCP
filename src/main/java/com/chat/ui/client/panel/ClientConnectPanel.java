package com.chat.ui.client.panel;

import com.chat.model.ClientViewModel;
import com.chat.ui.client.action.ConnectAction;
import com.chat.ui.client.action.DisconnectAction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ClientConnectPanel extends JPanel {
    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JTextField portField = new JTextField("5555");
    private final JLabel statusLabel = new JLabel("Trạng thái: ");
    private final JButton connectBtn = new JButton();
    //ĐÃ XÓA: private final JButton disconnectBtn = new JButton();

    private final ClientViewModel viewModel;
    //ĐÃ THÊM VÀ KHẮC PHỤC LỖI KHÔNG KHỞI TẠO (Initialized)
    private final Action connectAction;
    private final Action disconnectAction;

    public ClientConnectPanel(ClientViewModel viewModel, Action connectAction, Action disconnectAction) {
        this.viewModel = viewModel;
        this.connectAction = connectAction; //FIX: Khởi tạo connectAction
        this.disconnectAction = disconnectAction; //FIX: Khởi tạo disconnectAction

        // SỬA: Dùng BorderLayout để kiểm soát vị trí Status và Controls
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 15, 5, 15));

        // 1. Status Panel (Bên trái)
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.WEST);

        // 2. Controls Panel (Bên phải)
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        // Khởi tạo nút là Connect
        connectBtn.setAction(connectAction);
        connectBtn.setText("Kết nối");

        controlsPanel.add(new JLabel("Host:"));
        hostField.setColumns(8);
        controlsPanel.add(hostField);
        controlsPanel.add(new JLabel("Port:"));
        portField.setColumns(4);
        controlsPanel.add(portField);
        controlsPanel.add(connectBtn);

        add(controlsPanel, BorderLayout.EAST);

        // Liên kết View với Model
        viewModel.onStatusUpdate(this::setStatusLabel);
        viewModel.onStatusUpdate(this::updateButtonStates);
    }

    public void setStatusLabel(String status) {
        statusLabel.setText(status);
    }

    // ⭐️ LOGIC MỚI: Xử lý nút Connect/Disconnect duy nhất
    private void updateButtonStates(String status) {
        boolean connected = viewModel.isConnected();

        connectBtn.setEnabled(true);

        if (connected) {
            connectBtn.setAction(disconnectAction);
            connectBtn.setText("Ngắt kết nối");
            hostField.setEnabled(false);
            portField.setEnabled(false);
            // Thêm màu sắc nhấn (Accent Color) cho nút Ngắt kết nối (FlatLaf)
            connectBtn.putClientProperty("JButton.buttonType", "danger");
        } else {
            connectBtn.setAction(connectAction);
            connectBtn.setText("Kết nối");
            hostField.setEnabled(true);
            portField.setEnabled(true);
            // Thiết lập nút mặc định (Primary button) cho Connect (FlatLaf)
            connectBtn.putClientProperty("JButton.buttonType", "default");
            connectBtn.putClientProperty("JButton.defaultButtonFollowsState", true);
        }
    }

    public String getHost() { return hostField.getText(); }
    public String getPort() { return portField.getText(); }
}