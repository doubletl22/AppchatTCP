package com.chat.ui.client.dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class GifPickerDialog extends JDialog {
    // Danh sách GIF mẫu (sau này bạn có thể thay bằng API Giphy thật)
    private static final Map<String, String> SAMPLE_GIFS = new HashMap<>();
    static {
        SAMPLE_GIFS.put("Hello", "https://media.giphy.com/media/Cmr1OMJ2FN0B2/giphy.gif");
        SAMPLE_GIFS.put("Happy", "https://media.giphy.com/media/chzz1FQgqhytWRWbp3/giphy.gif");
        SAMPLE_GIFS.put("Cry", "https://media.giphy.com/media/OPU6wzx8JrHna/giphy.gif");
        SAMPLE_GIFS.put("Laugh", "https://media.giphy.com/media/9t6xpYZ9npJbG/giphy.gif");
        SAMPLE_GIFS.put("Thumbs Up", "https://media.giphy.com/media/XreQmk7ETCQtu/giphy.gif");
        SAMPLE_GIFS.put("Party", "https://media.giphy.com/media/3o7qDEq2bMbcbPRQ2c/giphy.gif");
        SAMPLE_GIFS.put("Dance", "https://media.giphy.com/media/l3V0lsGtTMSB5YNgc/giphy.gif");
        SAMPLE_GIFS.put("Bye", "https://media.giphy.com/media/26u4b45b8adiRdmr6/giphy.gif");
    }

    public GifPickerDialog(JFrame parent, Consumer<String> onGifSelected) {
        super(parent, "Chọn GIF", true);
        setLayout(new BorderLayout());
        setSize(500, 400);
        setLocationRelativeTo(parent);

        JPanel gridPanel = new JPanel(new GridLayout(0, 2, 5, 5)); // Lưới 2 cột

        SAMPLE_GIFS.forEach((name, url) -> {
            JPanel item = createGifItem(name, url, onGifSelected);
            gridPanel.add(item);
        });

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Đóng");
        closeBtn.addActionListener(e -> setVisible(false));
        add(closeBtn, BorderLayout.SOUTH);
    }

    private JPanel createGifItem(String name, String urlStr, Consumer<String> onSelect) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        panel.setBackground(Color.WHITE);
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel nameLabel = new JLabel(name, SwingConstants.CENTER);
        JLabel imageLabel = new JLabel("Loading...", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(150, 120));

        // Tải ảnh thumbnail (bất đồng bộ để không đơ UI)
        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                ImageIcon icon = new ImageIcon(url);
                // Scale ảnh nhỏ lại cho thumbnail
                Image img = icon.getImage().getScaledInstance(150, 120, Image.SCALE_DEFAULT);
                ImageIcon scaledIcon = new ImageIcon(img);

                SwingUtilities.invokeLater(() -> {
                    imageLabel.setText("");
                    imageLabel.setIcon(scaledIcon);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> imageLabel.setText("Lỗi ảnh"));
            }
        }).start();

        panel.add(imageLabel, BorderLayout.CENTER);
        panel.add(nameLabel, BorderLayout.SOUTH);

        // Xử lý sự kiện click chọn GIF
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onSelect.accept(urlStr); // Trả về URL của ảnh
                setVisible(false); // Đóng dialog
            }
        });

        return panel;
    }
}