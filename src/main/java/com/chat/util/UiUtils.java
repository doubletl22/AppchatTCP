package com.chat.util;

import com.formdev.flatlaf.FlatLightLaf; // Đổi từ FlatDarkLaf sang FlatLightLaf
import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UiUtils {
    public static final String DATE_FORMAT = "HH:mm:ss";
    public static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat(DATE_FORMAT);

    // ĐỊNH NGHĨA MÀU SẮC CHỦ ĐẠO TẠI ĐÂY ĐỂ DÙNG CHUNG
    public static final Color TEAL_COLOR = new Color(0, 150, 136); // Màu Xanh Ngọc
    public static final Color OFF_WHITE = new Color(245, 247, 251); // Màu nền xám rất nhạt cho đẹp

    public static void invokeLater(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    public static void setupLookAndFeel() {
        try {
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");

            UIManager.put("Button.arc", 15);
            UIManager.put("Component.arc", 15);
            UIManager.put("TextComponent.arc", 15);

            // --- CẤU HÌNH MÀU SẮC MỚI ---
            UIManager.put("Component.accentColor", TEAL_COLOR);
            UIManager.put("Button.background", TEAL_COLOR);
            UIManager.put("Button.foreground", Color.WHITE);

            // Padding rộng rãi hơn
            UIManager.put("TextField.margin", new Insets(8, 12, 8, 12));
            UIManager.put("Button.margin", new Insets(8, 20, 8, 20));

            // SỬ DỤNG GIAO DIỆN SÁNG (LIGHT THEME)
            FlatLightLaf.setup();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static JLabel createSystemMessageLabel(String text) {
        String displayTime = "[" + TIME_FORMATTER.format(new Date()) + "] ";
        JLabel systemLabel = new JLabel(displayTime + text, SwingConstants.CENTER);
        systemLabel.setForeground(Color.GRAY); // Màu chữ xám trên nền sáng
        systemLabel.setFont(systemLabel.getFont().deriveFont(Font.ITALIC, 12f));
        systemLabel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        return systemLabel;
    }
}