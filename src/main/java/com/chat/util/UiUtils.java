package com.chat.util;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UiUtils {
    public static final String DATE_FORMAT = "HH:mm:ss";
    public static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat(DATE_FORMAT);
    public static final Color TEAL_COLOR = new Color(0, 150, 136); // Màu chủ đạo

    private static boolean isDarkMode = false; // Mặc định là Sáng

    public static void invokeLater(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) runnable.run();
        else SwingUtilities.invokeLater(runnable);
    }

    public static void setupLookAndFeel() {
        // Cấu hình chung
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        UIManager.put("Button.arc", 15);
        UIManager.put("Component.arc", 15);
        UIManager.put("TextComponent.arc", 15);
        UIManager.put("Component.accentColor", TEAL_COLOR);

        // Khởi tạo theme dựa trên biến isDarkMode
        applyTheme(false);
    }

    public static void toggleTheme() {
        isDarkMode = !isDarkMode;
        applyTheme(true);
    }

    private static void applyTheme(boolean updateUI) {
        try {
            if (isDarkMode) {
                FlatDarkLaf.setup();
                // --- BẢNG MÀU DARK MODE ---
                UIManager.put("App.selfMessageBackground", new Color(0, 121, 107)); // Teal tối hơn
                UIManager.put("App.selfMessageForeground", Color.WHITE);
                UIManager.put("App.otherMessageBackground", new Color(60, 63, 65));
                UIManager.put("App.otherMessageForeground", new Color(220, 220, 220));
                UIManager.put("App.inputBackground", new Color(43, 43, 43));
            } else {
                FlatLightLaf.setup();
                // --- BẢNG MÀU LIGHT MODE ---
                UIManager.put("App.selfMessageBackground", TEAL_COLOR);
                UIManager.put("App.selfMessageForeground", Color.WHITE);
                UIManager.put("App.otherMessageBackground", new Color(230, 230, 230));
                UIManager.put("App.otherMessageForeground", Color.BLACK);
                UIManager.put("App.inputBackground", new Color(240, 242, 245));
            }

            if (updateUI) {
                // Phép màu của FlatLaf: Tự động vẽ lại toàn bộ ứng dụng
                FlatLaf.updateUI();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static JLabel createSystemMessageLabel(String text) {
        String displayTime = "[" + TIME_FORMATTER.format(new Date()) + "] ";
        JLabel systemLabel = new JLabel(displayTime + text, SwingConstants.CENTER);
        // Tự động chỉnh màu chữ theo theme (Label.disabledForeground thường là màu xám đẹp)
        systemLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        systemLabel.setFont(systemLabel.getFont().deriveFont(Font.ITALIC, 12f));
        systemLabel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        return systemLabel;
    }
}