package com.chat.ui.client.dialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginDialog extends JDialog {
    private final JTextField hostField = new JTextField("127.0.0.1", 15);
    private final JTextField portField = new JTextField("5555", 15);
    private final JTextField usernameField = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);

    private boolean cancelled = true;
    private String username;
    private String password;
    private String action;

    public LoginDialog(JFrame parent) {
        super(parent, "Đăng nhập hoặc đăng kí", true);
        // Tăng số hàng để chứa Host và Port
        // thay đổi gridlayuot
        setLayout(new GridLayout(6, 2, 8, 8));
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(20, 15, 15, 15));// đổi thông số

        //Thêm host và port
        add(new JLabel("Host:"));
        add(hostField);
        add(new JLabel("Port:"));
        add(portField);

        add(new JLabel("Tên đăng nhập:"));
        add(usernameField);
        add(new JLabel("Mật khẩu:"));
        add(passwordField);

        JButton loginBtn = new JButton("Đăng nhập");
        JButton registerBtn = new JButton("Đăng kí");

        //Thêm: Biến nút login thành nút Primary (Flatflar)
        loginBtn.putClientProperty("JButton.defaultButtonFollowsState", true);
        loginBtn.putClientProperty("JButton.buttonType", "default");

        loginBtn.addActionListener(e -> completeAction("login"));
        registerBtn.addActionListener(e -> completeAction("register"));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        buttonPanel.add(loginBtn);
        buttonPanel.add(registerBtn);

        add(buttonPanel);
        add(new JLabel());


        pack();
        setSize(300, getHeight());//tăng chều rộng
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void completeAction(String action) {
        this.username = usernameField.getText();
        this.password = new String(passwordField.getPassword());
        this.action = action;
        this.cancelled = false;
        setVisible(false);
    }

    public boolean isCancelled() { return cancelled; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getAction() { return action; }
    public String getHost() { return hostField.getText(); }

    public int getPort() {
        try {
            return Integer.parseInt(portField.getText());
        } catch (NumberFormatException e) {
            return -1; // Controller sẽ xử lý lỗi này
        }
    }
}