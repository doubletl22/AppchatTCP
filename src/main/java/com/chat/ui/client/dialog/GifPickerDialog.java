package com.chat.ui.client.dialog;

import com.chat.service.GifService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

public class GifPickerDialog extends JDialog {
    private final GifService gifService = new GifService();
    private final JPanel gridPanel;
    private final Consumer<String> onGifSelected;
    private final JScrollPane scrollPane;

    public GifPickerDialog(JFrame parent, Consumer<String> onGifSelected) {
        super(parent, "Kho GIF Online (Tenor)", true);
        this.onGifSelected = onGifSelected;
        setLayout(new BorderLayout(10, 10));
        setSize(620, 500);
        setLocationRelativeTo(parent);
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- 1. THANH TÌM KIẾM ---
        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        JTextField searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Tìm kiếm GIF (ví dụ: haha, cute cat)...");
        searchField.putClientProperty("Component.arc", 10);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JButton searchBtn = new JButton("Tìm");
        searchBtn.putClientProperty("JButton.buttonType", "roundRect");

        // Sự kiện tìm kiếm
        searchBtn.addActionListener(e -> loadGifs(searchField.getText()));
        searchField.addActionListener(e -> loadGifs(searchField.getText())); // Enter để tìm

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchBtn, BorderLayout.EAST);

        add(searchPanel, BorderLayout.NORTH);

        // --- 2. GRID HIỂN THỊ ẢNH ---
        // Grid 3 cột, khoảng cách 10px
        gridPanel = new JPanel(new GridLayout(0, 3, 10, 10));
        gridPanel.setBackground(Color.WHITE);

        scrollPane = new JScrollPane(gridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        add(scrollPane, BorderLayout.CENTER);

        // Load mặc định (Trending) khi mở lên
        loadGifs("");
    }

    private void loadGifs(String query) {
        // Hiển thị trạng thái đang tải
        gridPanel.removeAll();
        gridPanel.add(new JLabel("Đang tải...", SwingConstants.CENTER));
        gridPanel.revalidate();
        gridPanel.repaint();

        // Chạy ngầm để không đơ giao diện
        new Thread(() -> {
            List<String> urls;
            if (query == null || query.trim().isEmpty()) {
                urls = gifService.getTrendingGifs(21); // Lấy 21 ảnh hot
            } else {
                urls = gifService.searchGifs(query, 21); // Tìm kiếm
            }

            SwingUtilities.invokeLater(() -> {
                gridPanel.removeAll();

                if (urls.isEmpty()) {
                    gridPanel.add(new JLabel("Không tìm thấy ảnh nào!", SwingConstants.CENTER));
                } else {
                    for (String url : urls) {
                        JPanel item = createGifItem(url);
                        gridPanel.add(item);
                    }
                }
                gridPanel.revalidate();
                gridPanel.repaint();
            });
        }).start();
    }

    private JPanel createGifItem(String urlStr) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.LIGHT_GRAY);
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // Kích thước cố định cho mỗi ô ảnh
        panel.setPreferredSize(new Dimension(180, 120));

        JLabel imageLabel = new JLabel("Loading...", SwingConstants.CENTER);
        panel.add(imageLabel, BorderLayout.CENTER);

        // Tải ảnh thumbnail
        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                ImageIcon icon = new ImageIcon(url);
                // Resize ảnh cho vừa khung grid (180x120)
                Image img = icon.getImage().getScaledInstance(180, 120, Image.SCALE_DEFAULT);
                ImageIcon scaledIcon = new ImageIcon(img);

                SwingUtilities.invokeLater(() -> {
                    imageLabel.setText("");
                    imageLabel.setIcon(scaledIcon);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> imageLabel.setText("Lỗi"));
            }
        }).start();

        // Click vào ảnh để chọn
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onGifSelected.accept(urlStr);
                setVisible(false); // Đóng dialog
            }

            // Hiệu ứng Hover
            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setBorder(BorderFactory.createLineBorder(new Color(0, 122, 255), 3));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                panel.setBorder(null);
            }
        });

        return panel;
    }
}