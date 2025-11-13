package com.chat.util;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.formdev.flatlaf.FlatDarkLaf;

public class UiUtils {
    public static final String DATE_FORMAT = "HH:mm:ss";
    public static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat(DATE_FORMAT);

    public static void invokeLater(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    public static void setupLookAndFeel() {
        try {
            //thêm các thuộc tính render hints cho chu mượt hơn
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf: " + ex);
        }
    }

    public static JLabel createSystemMessageLabel(String text) {
        String displayTime = "[" + TIME_FORMATTER.format(new Date()) + "] ";
        JLabel systemLabel = new JLabel(displayTime + text, SwingConstants.CENTER);
        systemLabel.setForeground(new Color(150, 150, 150));
        systemLabel.setFont(systemLabel.getFont().deriveFont(Font.ITALIC, 11f));
        systemLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return systemLabel;
    }
}