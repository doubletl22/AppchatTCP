package com.chat.util;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UiUtils {
    public static final String DATE_FORMAT = "HH:mm:ss";
    public static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat(DATE_FORMAT);

    /**
     * Helper để chạy code trên luồng giao diện (EDT) an toàn
     */
    public static void invokeLater(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    /**
     * Thiết lập giao diện FlatLaf với các tùy chỉnh hiện đại
     */
    public static void setupLookAndFeel() {
        try {
            // 1. Cấu hình khử răng cưa cho font chữ (Anti-aliasing)
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");

            // 2. Cấu hình Bo góc (Rounding) cho các thành phần UI
            // Giá trị càng lớn thì góc càng tròn (đơn vị: pixel)
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
            UIManager.put("ProgressBar.arc", 12);
            UIManager.put("TextComponent.arc", 12);

            // 3. Cấu hình Màu chủ đạo (Accent Color)
            // Sử dụng màu xanh dương tươi (kiểu Messenger/Apple) thay vì màu mặc định
            UIManager.put("Component.accentColor", new Color(0, 122, 255));
            // Màu khi focus vào input/button
            UIManager.put("Component.focusWidth", 1); // Viền focus mỏng lại cho tinh tế
            UIManager.put("Component.innerFocusWidth", 0);

            // 4. Cấu hình Padding (Khoảng cách nội dung bên trong)
            // Giúp Text Field và Button trông thoáng hơn
            UIManager.put("TextField.margin", new Insets(6, 10, 6, 10)); // Trên, Trái, Dưới, Phải
            UIManager.put("Button.margin", new Insets(6, 16, 6, 16));

            // 5. Cài đặt font chữ mặc định (Tùy chọn, Segoe UI cho Windows trông đẹp hơn)
            // Nếu muốn dùng font mặc định của hệ thống thì bỏ qua dòng này
            // UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));

            // 6. Kích hoạt FlatLaf Dark Mode
            FlatDarkLaf.setup();

        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf: " + ex);
            // Fallback về giao diện mặc định nếu lỗi
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }
    }

    /**
     * Tạo Label hiển thị thông báo hệ thống (căn giữa, màu xám)
     */
    public static JLabel createSystemMessageLabel(String text) {
        String displayTime = "[" + TIME_FORMATTER.format(new Date()) + "] ";
        JLabel systemLabel = new JLabel(displayTime + text, SwingConstants.CENTER);

        // Màu xám nhạt để không gây chú ý quá mức
        systemLabel.setForeground(new Color(150, 150, 150));

        // Font in nghiêng nhỏ hơn chút
        systemLabel.setFont(systemLabel.getFont().deriveFont(Font.ITALIC, 11f));

        // Thêm khoảng cách trên dưới để tách biệt với tin nhắn chat
        systemLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        return systemLabel;
    }
}