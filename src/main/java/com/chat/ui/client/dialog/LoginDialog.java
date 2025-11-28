package com.chat.ui.client.dialog;

import com.chat.util.UiUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginDialog extends JDialog {
    // Vẫn giữ biến field nhưng không add vào giao diện để Controller vẫn lấy được dữ liệu mặc định
    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JTextField portField = new JTextField("5555");

    private final JTextField usernameField = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);

    // Nút bấm
    private final JButton mainActionBtn = new JButton();
    private final JButton switchModeBtn = new JButton();

    private boolean cancelled = true;
    private String username;
    private String password;
    private String action; // "login", "register", hoặc "switch"

    private final boolean isRegisterMode;

    public LoginDialog(JFrame parent, boolean isRegisterMode) {
        super(parent, isRegisterMode ? "Đăng ký tài khoản mới" : "Đăng nhập", true);
        this.isRegisterMode = isRegisterMode;

        setLayout(new GridBagLayout());
        JPanel contentPanel = (JPanel) getContentPane();
        contentPanel.setBorder(new EmptyBorder(25, 30, 25, 30)); // Căn lề rộng rãi
        contentPanel.setBackground(new Color(245, 247, 251)); // Màu nền nhẹ nhàng

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0); // Khoảng cách dọc

        // --- LOGO / TITLE ---
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel titleLabel = new JLabel(isRegisterMode ? "TẠO TÀI KHOẢN" : "CHÀO MỪNG TRỞ LẠI");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(UiUtils.TEAL_COLOR);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, gbc);

        gbc.gridy++;
        add(Box.createVerticalStrut(10), gbc);

        // --- INPUTS ---
        // 1. Username
        gbc.gridy++;
        add(new JLabel("Tên đăng nhập:"), gbc);
        gbc.gridy++;
        styleTextField(usernameField);
        add(usernameField, gbc);

        // 2. Password
        gbc.gridy++;
        add(new JLabel("Mật khẩu:"), gbc);
        gbc.gridy++;
        styleTextField(passwordField);
        add(passwordField, gbc);

        // --- BUTTONS ---
        gbc.gridy++;
        add(Box.createVerticalStrut(15), gbc);
        gbc.gridy++;

        // Cấu hình nút chính (Đăng nhập hoặc Đăng ký)
        mainActionBtn.setText(isRegisterMode ? "Đăng ký ngay" : "Đăng nhập");
        mainActionBtn.setBackground(UiUtils.TEAL_COLOR);
        mainActionBtn.setForeground(Color.WHITE);
        mainActionBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        mainActionBtn.setFocusPainted(false);
        mainActionBtn.putClientProperty("JButton.buttonType", "roundRect"); // FlatLaf style
        mainActionBtn.setPreferredSize(new Dimension(200, 35));

        mainActionBtn.addActionListener(e -> {
            if(validateInput()) {
                completeAction(isRegisterMode ? "register" : "login");
            }
        });
        add(mainActionBtn, gbc);

        // Cấu hình nút chuyển chế độ (Link text)
        gbc.gridy++;
        switchModeBtn.setText(isRegisterMode ? "<html><u>Đã có tài khoản? Đăng nhập</u></html>" : "<html><u>Chưa có tài khoản? Đăng ký ngay</u></html>");
        switchModeBtn.setBorderPainted(false);
        switchModeBtn.setContentAreaFilled(false);
        switchModeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        switchModeBtn.setForeground(new Color(0, 100, 200));

        switchModeBtn.addActionListener(e -> {
            this.action = "switch"; // Báo hiệu cho Controller biết cần đổi bảng
            this.cancelled = false;
            setVisible(false);
        });
        add(switchModeBtn, gbc);

        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void styleTextField(JTextField field) {
        field.putClientProperty("Component.arc", 10); // Bo góc
        field.putClientProperty("JComponent.roundRect", true);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                new EmptyBorder(8, 10, 8, 10))
        );
    }

    private boolean validateInput() {
        if (usernameField.getText().trim().isEmpty() || new String(passwordField.getPassword()).trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void completeAction(String action) {
        this.username = usernameField.getText().trim();
        this.password = new String(passwordField.getPassword());
        this.action = action;
        this.cancelled = false;
        setVisible(false);
    }

    public boolean isCancelled() { return cancelled; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getAction() { return action; }
    // Vẫn trả về mặc định để Controller không lỗi
    public String getHost() { return "127.0.0.1"; }
    public int getPort() { return 5555; }
}