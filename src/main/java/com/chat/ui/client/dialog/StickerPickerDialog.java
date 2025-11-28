package com.chat.ui.client.dialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.util.function.Consumer;

public class StickerPickerDialog extends JDialog {
    private final Consumer<String> onStickerSelected;

    // [CẤU HÌNH] Danh sách tên các bộ Sticker (trùng tên thư mục trong resources/stickers)
    // Dùng để dự phòng khi không quét được thư mục tự động
    private static final String[] PACK_NAMES = {
            "Tuzki", "QooBee Agap", "QooBee Agap 2", "dr_meep"
    };

    public StickerPickerDialog(JFrame parent, Consumer<String> onStickerSelected) {
        super(parent, "Chọn Sticker", true);
        this.onStickerSelected = onStickerSelected;

        setLayout(new BorderLayout());
        setSize(600, 500); // Tăng kích thước một chút cho thoải mái
        setLocationRelativeTo(parent);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        loadStickerPacks(tabbedPane);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void loadStickerPacks(JTabbedPane tabbedPane) {
        boolean success = false;

        // --- CÁCH 1: QUÉT TỰ ĐỘNG (Dùng cho môi trường IDE/File System) ---
        try {
            URL url = getClass().getResource("/stickers");
            if (url != null && "file".equals(url.getProtocol())) {
                File stickersDir = new File(url.toURI());
                File[] packs = stickersDir.listFiles(File::isDirectory);

                if (packs != null) {
                    for (File pack : packs) {
                        JPanel gridPanel = createGridPanel();
                        int count = 0;

                        // Quét file ảnh trong thư mục
                        File[] images = pack.listFiles((dir, name) -> isImageFile(name));
                        if (images != null) {
                            for (File img : images) {
                                String path = "/stickers/" + pack.getName() + "/" + img.getName();
                                gridPanel.add(createStickerItem(path));
                                count++;
                            }
                        }

                        if (count > 0) {
                            addTabWithScroll(tabbedPane, pack.getName(), gridPanel);
                        }
                    }
                    success = true;
                }
            }
        } catch (Exception ignored) {
            System.err.println("Chuyển sang chế độ load thủ công.");
        }

        if (success) return;

        // --- CÁCH 2: LOAD THỦ CÔNG (Dùng cho file .JAR và tên file phức tạp) ---
        for (String packName : PACK_NAMES) {
            JPanel gridPanel = createGridPanel();
            int foundCount = 0;

            // Quét số thứ tự từ 0 đến 100
            for (int i = 0; i <= 100; i++) {

                // 1. Thử tên chuẩn: 1.png, 2.jpg...
                if (checkAndAdd(gridPanel, packName, i + ".png")) foundCount++;
                else if (checkAndAdd(gridPanel, packName, i + ".jpg")) foundCount++;
                else if (checkAndAdd(gridPanel, packName, i + ".gif")) foundCount++;

                // 2. Thử tên có số 0 ở đầu: 01.png, 05.jpg...
                String zeroPad = String.format("%02d", i);
                if (checkAndAdd(gridPanel, packName, zeroPad + ".png")) foundCount++;
                else if (checkAndAdd(gridPanel, packName, zeroPad + ".jpg")) foundCount++;

                    // 3. [QUAN TRỌNG] Hỗ trợ tên file kiểu Windows copy: "1 (1).png", "1 (2).png"...
                else if (checkAndAdd(gridPanel, packName, "1 (" + i + ").png")) foundCount++;
                else if (checkAndAdd(gridPanel, packName, "1 (" + i + ").jpg")) foundCount++;

                    // 4. Hỗ trợ đuôi lạ khác
                else if (checkAndAdd(gridPanel, packName, i + ".jpeg")) foundCount++;
                else if (checkAndAdd(gridPanel, packName, i + ".webp.png")) foundCount++;
            }

            if (foundCount > 0) {
                addTabWithScroll(tabbedPane, packName, gridPanel);
            }
        }

        if (tabbedPane.getTabCount() == 0) {
            tabbedPane.addTab("Thông báo", new JLabel("Không tìm thấy sticker nào", SwingConstants.CENTER));
        }
    }

    // Helper: Thử thêm ảnh vào panel, trả về true nếu ảnh tồn tại
    private boolean checkAndAdd(JPanel panel, String packName, String fileName) {
        String path = "/stickers/" + packName + "/" + fileName;
        if (getClass().getResource(path) != null) {
            panel.add(createStickerItem(path));
            return true;
        }
        return false;
    }

    private JPanel createGridPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 5, 5, 5)); // 5 cột
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        return panel;
    }

    private void addTabWithScroll(JTabbedPane tab, String title, JPanel content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(null);
        tab.addTab(title, scroll);
    }

    private boolean isImageFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".gif");
    }

    private JPanel createStickerItem(String resourcePath) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        panel.setPreferredSize(new Dimension(70, 70));

        URL imgUrl = getClass().getResource(resourcePath);
        if (imgUrl != null) {
            ImageIcon icon = new ImageIcon(imgUrl);
            Image img = icon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
            JLabel label = new JLabel(new ImageIcon(img));
            panel.add(label, BorderLayout.CENTER);
        }

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onStickerSelected.accept(resourcePath);
                setVisible(false);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setBackground(new Color(230, 230, 230));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                panel.setBackground(Color.WHITE);
            }
        });
        return panel;
    }
}