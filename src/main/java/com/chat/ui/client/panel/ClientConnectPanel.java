package com.chat.ui.client.panel;

import com.chat.model.ClientViewModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ClientConnectPanel extends JPanel {
    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JTextField portField = new JTextField("5555");
    private final JLabel statusLabel = new JLabel("Trạng thái: Ngắt kết nối");
    private final JButton connectBtn = new JButton();
    private final JButton disconnectBtn = new JButton();

    private final ClientViewModel viewModel;

    public ClientConnectPanel(ClientViewModel viewModel, Action connectAction, Action disconnectAction) {
        this.viewModel = viewModel;
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        setBorder(new EmptyBorder(0, 10, 0, 10));

        connectBtn.setAction(connectAction);
        disconnectBtn.setAction(disconnectAction);

        disconnectBtn.setEnabled(false);

        add(statusLabel);
        add(new JLabel("Host:"));
        hostField.setColumns(8);
        add(hostField);
        add(new JLabel("Port:"));
        portField.setColumns(4);
        add(portField);
        add(connectBtn);
        add(disconnectBtn);

        // Liên kết View với Model
        viewModel.onStatusUpdate(this::setStatusLabel);
        viewModel.onStatusUpdate(this::updateButtonStates);
    }

    public void setStatusLabel(String status) {
        statusLabel.setText(status);
    }

    private void updateButtonStates(String status) {
        boolean connected = viewModel.isConnected();
        connectBtn.setEnabled(!connected);
        disconnectBtn.setEnabled(connected);
    }

    public String getHost() { return hostField.getText(); }
    public String getPort() { return portField.getText(); }
}