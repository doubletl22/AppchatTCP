package com.chat.ui.client;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public class Toast extends JWindow {

    public Toast(JFrame parent, String message) {
        super(parent);
        // QUAN TRỌNG: Luôn hiển thị trên cùng
        setAlwaysOnTop(true);

        // Thiết kế giao diện đơn giản, chắc chắn hiển thị
        JPanel panel = new JPanel();
        panel.setBackground(new Color(33, 33, 33)); // Màu nền xám đen
        panel.setBorder(new LineBorder(new Color(0, 120, 215), 2)); // Viền xanh
        panel.setLayout(new BorderLayout(10, 10));

        // Icon đơn giản (dùng ký tự HTML để giả lập icon)
        JLabel lblIcon = new JLabel(" 💬 ");
        lblIcon.setForeground(Color.WHITE);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        panel.add(lblIcon, BorderLayout.WEST);

        // Nội dung tin nhắn
        JLabel lblMsg = new JLabel("<html><body style='width: 200px; color: white'>" + message + "</body></html>");
        lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(lblMsg, BorderLayout.CENTER);

        add(panel);
        pack(); // Tự động co giãn kích thước

        // Tính toán vị trí: Góc dưới bên phải của phần mềm Chat
        if (parent != null && parent.isVisible()) {
            try {
                Point loc = parent.getLocationOnScreen();
                int x = loc.x + parent.getWidth() - getWidth() - 20;
                int y = loc.y + parent.getHeight() - getHeight() - 20;
                setLocation(x, y);
            } catch (Exception e) {
                setLocationRelativeTo(null); // Fallback ra giữa màn hình nếu lỗi
            }
        } else {
            setLocationRelativeTo(null);
        }

        // Tự động tắt sau 4 giây
        new Thread(() -> {
            try {
                Thread.sleep(4000);
                SwingUtilities.invokeLater(this::dispose);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void show(JFrame parent, String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                // In ra console để debug xem code có chạy đến đây không
                System.out.println("[TOAST DEBUG] Đang hiển thị thông báo: " + message);

                Toast toast = new Toast(parent, message);
                toast.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}