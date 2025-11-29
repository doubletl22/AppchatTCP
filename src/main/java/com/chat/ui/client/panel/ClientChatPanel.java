package com.chat.ui.client.panel;

import com.chat.model.ClientViewModel;
import com.chat.model.Message;
import com.chat.ui.client.ClientController;
import com.chat.ui.client.dialog.GifPickerDialog;
import com.chat.ui.client.dialog.StickerPickerDialog;
import com.chat.util.AudioUtils;
import com.chat.util.UiUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Base64;
import java.util.Random;

public class ClientChatPanel extends JPanel {

    private final JPanel chatDisplayPanel = new JPanel(new GridBagLayout());
    private final RoundedTextField inputField = new RoundedTextField(20);
    private final JButton sendBtn = new JButton();

    // Các nút chức năng
    private final JButton micBtn = new JButton();
    private final JButton imageBtn = new JButton();
    private final JButton stickerBtn = new JButton();
    private final JButton gifBtn = new JButton();
    private final JButton emojiBtn = new JButton();

    private final AudioUtils audioRecorder = new AudioUtils();
    private final ClientViewModel viewModel;
    private ClientController controller;

    public ClientChatPanel(ClientViewModel viewModel, Action sendAction) {
        this.viewModel = viewModel;
        setLayout(new BorderLayout(0, 0));

        // --- 1. KHU VỰC HIỂN THỊ CHAT ---
        chatDisplayPanel.setOpaque(false);
        JScrollPane chatScroll = new JScrollPane(chatDisplayPanel);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);
        chatScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // Padding trên dưới cho vùng chat thoáng hơn
        chatScroll.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        chatScroll.getViewport().setOpaque(false);
        chatScroll.setOpaque(false);
        add(chatScroll, BorderLayout.CENTER);

        // --- 2. THANH NHẬP LIỆU ---
        JPanel bottomInput = new JPanel(new GridBagLayout());
        bottomInput.setBorder(new EmptyBorder(10, 10, 10, 10));
        bottomInput.setOpaque(true);
        bottomInput.setBackground(UIManager.getColor("Panel.background"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 4, 0, 4);

        // --- A. CÁC NÚT ICON BÊN TRÁI ---
        setupIconButton(micBtn, new MicIcon(22, UiUtils.TEAL_COLOR));
        setupIconButton(imageBtn, new ImageIconShape(22, UiUtils.TEAL_COLOR));
        setupIconButton(stickerBtn, new StickerIcon(22, UiUtils.TEAL_COLOR));
        setupIconButton(gifBtn, new GifIcon(28, 18, UiUtils.TEAL_COLOR));

        // Logic Mic: Nhấn giữ để thu âm, thả ra để gửi
        micBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                micBtn.setIcon(new MicIcon(22, Color.RED)); // Đổi màu đỏ báo hiệu đang thu
                try { audioRecorder.startRecording(); } catch (Exception ex) { ex.printStackTrace(); }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                micBtn.setIcon(new MicIcon(22, UiUtils.TEAL_COLOR));
                // Chạy luồng riêng để xử lý và gửi file âm thanh
                new Thread(() -> {
                    String base64 = audioRecorder.stopRecording();
                    if (base64 != null && controller != null) {
                        controller.handleSendVoice(base64);
                    }
                }).start();
            }
        });
        // Ngăn chặn sự kiện click mặc định làm hỏng logic nhấn giữ
        micBtn.addActionListener(e -> {});

        imageBtn.addActionListener(e -> chooseAndSendImage());
        gifBtn.addActionListener(e -> showGifPicker());
        stickerBtn.addActionListener(e -> showStickerPicker());

        gbc.gridx = 0; bottomInput.add(micBtn, gbc);
        gbc.gridx = 1; bottomInput.add(imageBtn, gbc);
        gbc.gridx = 2; bottomInput.add(stickerBtn, gbc);
        gbc.gridx = 3; bottomInput.add(gifBtn, gbc);

        // --- B. Ô NHẬP LIỆU (VIÊN THUỐC) ---
        inputField.setAction(sendAction);
        inputField.putClientProperty("JTextField.placeholderText", "Nhập tin nhắn...");

        // [FIX] Sử dụng Segoe UI để hiển thị Tiếng Việt tốt nhất trên Windows
        // Hệ thống sẽ tự động tìm font Emoji dự phòng khi cần thiết.
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 10));

        setupIconButton(emojiBtn, new EmojiIcon(20, UiUtils.TEAL_COLOR));
        emojiBtn.addActionListener(e -> showEmojiPopup(emojiBtn));
        inputField.putClientProperty("JTextField.trailingComponent", emojiBtn);

        gbc.gridx = 4; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.BOTH;
        bottomInput.add(inputField, gbc);

        // --- C. NÚT GỬI ---
        sendBtn.setAction(sendAction);
        setupIconButton(sendBtn, new SendIcon(24, UiUtils.TEAL_COLOR));

        gbc.gridx = 5; gbc.weightx = 0; gbc.fill = GridBagConstraints.VERTICAL;
        bottomInput.add(sendBtn, gbc);

        add(bottomInput, BorderLayout.SOUTH);

        updateInputStyle();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (inputField != null) {
            updateInputStyle();
            inputField.repaint();
        }
    }

    private void updateInputStyle() {
        inputField.setForeground(UIManager.getColor("TextField.foreground"));
        inputField.setCaretColor(UIManager.getColor("TextField.caretForeground"));
    }

    private void setupIconButton(JButton btn, Icon icon) {
        btn.setIcon(icon);
        btn.setText("");
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(0,0,0,0));
    }

    // --- LOGIC HIỂN THỊ TIN NHẮN ---

    public void appendMessage(Message m, String currentUserName) {
        // Xác định tin nhắn của ai
        boolean isSelf = (m.name != null && m.name.startsWith("[TO ")) ||
                (m.name != null && m.name.equals(currentUserName));

        // Phân loại tin nhắn
        boolean isVoice = "voice".equals(m.type) || "dm_voice".equals(m.type);
        boolean isGif = "gif".equals(m.type) || "dm_gif".equals(m.type) ||
                "gif_history".equals(m.type) || "dm_gif_history".equals(m.type);
        boolean isImage = "image".equals(m.type) || "dm_image".equals(m.type);
        boolean isSticker = "sticker".equals(m.type) || "dm_sticker".equals(m.type) ||
                "sticker_history".equals(m.type) || "dm_sticker_history".equals(m.type);

        UiUtils.invokeLater(() -> {
            if ("system".equals(m.type)) {
                JLabel systemLabel = UiUtils.createSystemMessageLabel(m.text);
                GridBagConstraints gbc = createGBC(GridBagConstraints.CENTER);
                chatDisplayPanel.add(systemLabel, gbc);
            } else {
                JPanel messageBubble;

                if (isVoice) messageBubble = createVoiceBubble(m.data, isSelf);
                else if (isGif) messageBubble = createGifBubble(m.text, isSelf);
                else if (isImage) messageBubble = createImageBubble(m.data, isSelf);
                else if (isSticker) messageBubble = createStickerBubble(m.text, isSelf);
                else messageBubble = createChatBubble(m.text, isSelf);

                JPanel wrapper = new JPanel(new BorderLayout());
                wrapper.setOpaque(false);

                if (!isSelf && m.name != null && !m.name.equals("Public Chat") && !m.name.startsWith("[TO ")) {
                    JLabel nameLabel = new JLabel(m.name);
                    nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    nameLabel.setForeground(Color.GRAY);
                    nameLabel.setBorder(new EmptyBorder(0, 5, 2, 0));
                    wrapper.add(nameLabel, BorderLayout.NORTH);
                }
                wrapper.add(messageBubble, BorderLayout.CENTER);

                JPanel alignPanel = new JPanel(new FlowLayout(isSelf ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
                alignPanel.setOpaque(false);
                alignPanel.add(wrapper);

                GridBagConstraints gbc = createGBC(isSelf ? GridBagConstraints.EAST : GridBagConstraints.WEST);
                chatDisplayPanel.add(alignPanel, gbc);
            }
            updateFiller();
            chatDisplayPanel.revalidate();
            chatDisplayPanel.repaint();
            scrollToBottom();
        });
    }

    // 1. Bong bóng Chat Văn Bản
    private JPanel createChatBubble(String text, boolean isSelf) {
        BubblePanel bubble = new BubblePanel(isSelf);
        bubble.setLayout(new BorderLayout());

        JTextPane textPane = new JTextPane();
        textPane.setText(text);
        textPane.setEditable(false);
        textPane.setOpaque(false);
        // [FIX] Sử dụng Segoe UI để hiển thị tốt Tiếng Việt
        textPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textPane.setForeground(isSelf ? Color.WHITE : Color.BLACK);

        int width = Math.min(400, getFontMetrics(textPane.getFont()).stringWidth(text) + 30);
        textPane.setPreferredSize(new Dimension(width, textPane.getPreferredSize().height));

        bubble.add(textPane, BorderLayout.CENTER);
        return bubble;
    }

    // 2. Bong bóng Voice
    private JPanel createVoiceBubble(String base64Audio, boolean isSelf) {
        BubblePanel bubble = new BubblePanel(isSelf);
        bubble.setLayout(new BorderLayout(10, 0));
        bubble.setPreferredSize(new Dimension(220, 45));

        JButton playBtn = new JButton("▶");
        playBtn.setFont(new Font("Segoe UI Symbol", Font.BOLD, 14));
        playBtn.setForeground(isSelf ? UiUtils.TEAL_COLOR : Color.WHITE);
        playBtn.setBorderPainted(false);
        playBtn.setContentAreaFilled(false);
        playBtn.setFocusPainted(false);
        playBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel btnWrapper = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelf ? Color.WHITE : new Color(220, 220, 220));
                g2.fillOval(2, 2, 30, 30);
            }
        };
        btnWrapper.setLayout(new GridBagLayout());
        btnWrapper.setPreferredSize(new Dimension(35, 35));
        btnWrapper.setOpaque(false);
        btnWrapper.add(playBtn);

        playBtn.addActionListener(e -> {
            if (base64Audio != null) AudioUtils.playBase64Audio(base64Audio);
        });

        JPanel waveForm = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelf ? new Color(255,255,255, 180) : new Color(0,0,0, 100));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                Random rand = new Random(123);
                for (int i = 5; i < getWidth(); i += 6) {
                    int h = rand.nextInt(16) + 4;
                    int y = (getHeight() - h) / 2;
                    g2.drawLine(i, y, i, y + h);
                }
            }
        };
        waveForm.setOpaque(false);

        bubble.add(btnWrapper, BorderLayout.WEST);
        bubble.add(waveForm, BorderLayout.CENTER);
        return bubble;
    }

    // 3. Bong bóng Ảnh
    private JPanel createImageBubble(String base64, boolean isSelf) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel imgLabel = new JLabel("Đang tải ảnh...", SwingConstants.CENTER);
        imgLabel.setForeground(Color.GRAY);
        imgLabel.setPreferredSize(new Dimension(200, 150));
        panel.add(imgLabel);

        new Thread(() -> {
            try {
                if (base64 == null || base64.isEmpty()) return;
                byte[] btDataFile = Base64.getDecoder().decode(base64);
                BufferedImage rawImage = ImageIO.read(new ByteArrayInputStream(btDataFile));
                if (rawImage != null) {
                    int maxWidth = 250;
                    int w = rawImage.getWidth();
                    int h = rawImage.getHeight();
                    if (w > maxWidth) {
                        h = (h * maxWidth) / w;
                        w = maxWidth;
                    }
                    Image scaled = rawImage.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                    ImageIcon icon = new ImageIcon(scaled);

                    SwingUtilities.invokeLater(() -> {
                        imgLabel.setText("");
                        imgLabel.setIcon(icon);
                        imgLabel.setPreferredSize(null);
                        panel.revalidate(); panel.repaint();
                    });
                } else {
                    SwingUtilities.invokeLater(() -> imgLabel.setText("[Lỗi ảnh]"));
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> imgLabel.setText("[Lỗi tải ảnh]"));
            }
        }).start();
        return panel;
    }

    // 4. Bong bóng GIF
    private JPanel createGifBubble(String urlStr, boolean isSelf) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel gifLabel = new JLabel("Loading GIF...", SwingConstants.CENTER);
        gifLabel.setPreferredSize(new Dimension(200, 150));
        panel.add(gifLabel);

        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                ImageIcon icon = new ImageIcon(url);
                SwingUtilities.invokeLater(() -> {
                    gifLabel.setText("");
                    gifLabel.setIcon(icon);
                    gifLabel.setPreferredSize(null);
                    panel.revalidate(); panel.repaint();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> gifLabel.setText("[Lỗi GIF]"));
            }
        }).start();
        return panel;
    }

    // 5. Bong bóng Sticker
    private JPanel createStickerBubble(String stickerPath, boolean isSelf) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        int displaySize = 120;

        JLabel stickerLabel = new JLabel();
        stickerLabel.setHorizontalAlignment(SwingConstants.CENTER);

        try {
            URL url = getClass().getResource(stickerPath);
            if (url != null) {
                ImageIcon icon = new ImageIcon(url);
                Image img = icon.getImage().getScaledInstance(displaySize, displaySize, Image.SCALE_SMOOTH);
                stickerLabel.setIcon(new ImageIcon(img));
            } else {
                stickerLabel.setText("[Sticker lỗi]");
            }
        } catch (Exception e) {
            stickerLabel.setText("[Sticker lỗi]");
        }

        panel.add(stickerLabel, BorderLayout.CENTER);
        return panel;
    }

    // --- CÁC COMPONENT TÙY CHỈNH ---

    private static class BubblePanel extends JPanel {
        private final boolean isSelf;
        public BubblePanel(boolean isSelf) {
            this.isSelf = isSelf;
            setOpaque(false);
            setBorder(new EmptyBorder(10, 15, 10, 15));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelf) g2.setColor(UiUtils.TEAL_COLOR);
            else g2.setColor(new Color(230, 230, 230));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedTextField extends JTextField {
        private final int radius;
        public RoundedTextField(int radius) { this.radius = radius; setOpaque(false); }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bgColor = UIManager.getColor("TextField.background");
            if (getParent() != null && getParent().getBackground().getRed() > 200) bgColor = new Color(240, 242, 245);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, radius, radius);
            g2.setColor(new Color(200, 200, 200));
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // --- HELPER METHODS & ICONS ---
    private GridBagConstraints createGBC(int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0; gbc.anchor = anchor; gbc.insets = new Insets(2, 10, 2, 10);
        return gbc;
    }
    private void updateFiller() {
        GridBagConstraints fillerGBC = new GridBagConstraints();
        fillerGBC.gridwidth = GridBagConstraints.REMAINDER; fillerGBC.weighty = 1.0;
        fillerGBC.fill = GridBagConstraints.VERTICAL;
        Component verticalGlue = null;
        if (chatDisplayPanel.getComponentCount() > 0) {
            Component lastComponent = chatDisplayPanel.getComponent(chatDisplayPanel.getComponentCount() - 1);
            if (lastComponent instanceof Box.Filler) { verticalGlue = lastComponent; chatDisplayPanel.remove(verticalGlue); }
        }
        if (verticalGlue == null) verticalGlue = Box.createVerticalGlue();
        chatDisplayPanel.add(verticalGlue, fillerGBC);
    }
    private void scrollToBottom() {
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, chatDisplayPanel);
        if (scrollPane != null) {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        }
    }
    public void setController(ClientController controller) { this.controller = controller; }
    public String getInputText() { return inputField.getText(); }
    public void clearInputField() { inputField.setText(""); }
    public void clearChatDisplay() { UiUtils.invokeLater(() -> { chatDisplayPanel.removeAll(); chatDisplayPanel.revalidate(); chatDisplayPanel.repaint(); }); }
    private void chooseAndSendImage() { JFileChooser fc = new JFileChooser(); fc.setDialogTitle("Chọn ảnh"); fc.setFileFilter(new FileNameExtensionFilter("Ảnh", "jpg", "png", "gif")); if(fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION && controller!=null) controller.handleSendImage(fc.getSelectedFile()); }
    private void showGifPicker() { JFrame f=(JFrame)SwingUtilities.getWindowAncestor(this); new GifPickerDialog(f, url -> { inputField.setText("/gif "+url); if(controller!=null) controller.handleSend(inputField.getText()); clearInputField(); }).setVisible(true); }

    private void showStickerPicker() {
        JFrame f = (JFrame) SwingUtilities.getWindowAncestor(this);
        new StickerPickerDialog(f, stickerPath -> {
            if (controller != null) {
                controller.handleSendSticker(stickerPath);
            }
        }).setVisible(true);
    }

    private void showEmojiPopup(Component invoker) {
        JPopupMenu popup = new JPopupMenu(); popup.setBackground(Color.WHITE); popup.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        String[] emojis = {"😀", "😂", "🥰", "😎", "😭", "👍", "👎", "❤️", "🔥", "🎉"};
        JPanel panel = new JPanel(new GridLayout(2, 5, 5, 5)); panel.setOpaque(false);
        for (String emoji : emojis) {
            JButton btn = new JButton(emoji);
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
            btn.setBorderPainted(false); btn.setContentAreaFilled(false);
            btn.addActionListener(e -> { inputField.replaceSelection(emoji); popup.setVisible(false); });
            panel.add(btn);
        }
        popup.add(panel); popup.show(invoker, 0, -100);
    }

    private static class MicIcon implements Icon {
        private final int size; private final Color color;
        public MicIcon(int size, Color color) { this.size = size; this.color = color; }
        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }
        public void paintIcon(Component c, Graphics g, int x, int y) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(color); int w = size/3 + 2; int h = size/2; g2.fillRoundRect(x + (size-w)/2, y, w, h, w, w); g2.setStroke(new BasicStroke(2)); g2.drawArc(x + 2, y + 2, size - 5, size - 5, 180, 180); g2.drawLine(x + size/2, y + size - 3, x + size/2, y + size); g2.drawLine(x + size/3, y + size, x + size - size/3, y + size); g2.dispose(); }
    }
    private static class ImageIconShape implements Icon {
        private final int size; private final Color color;
        public ImageIconShape(int size, Color color) { this.size = size; this.color = color; }
        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }
        public void paintIcon(Component c, Graphics g, int x, int y) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(color); g2.setStroke(new BasicStroke(2)); g2.drawRoundRect(x, y + 2, size, size - 4, 4, 4); Path2D p = new Path2D.Double(); p.moveTo(x+2, y+size-4); p.lineTo(x+size/3.0, y+size/2.0); p.lineTo(x+size/2.0, y+size-4); p.lineTo(x+size/1.5, y+size/3.0); p.lineTo(x+size-2, y+size-4); g2.draw(p); g2.fillOval(x+size-8, y+5, 4, 4); g2.dispose(); }
    }
    private static class StickerIcon implements Icon {
        private final int size; private final Color color;
        public StickerIcon(int size, Color color) { this.size = size; this.color = color; }
        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }
        public void paintIcon(Component c, Graphics g, int x, int y) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(color); g2.setStroke(new BasicStroke(2)); g2.drawOval(x, y, size - 1, size - 1); g2.fillOval(x+5, y+7, 3, 3); g2.fillOval(x+size-8, y+7, 3, 3); g2.drawArc(x+4, y+8, size-9, size-12, 0, -180); g2.dispose(); }
    }
    private static class EmojiIcon extends StickerIcon { public EmojiIcon(int size, Color color) { super(size, color); } }
    private static class SendIcon implements Icon {
        private final int size; private final Color color;
        public SendIcon(int size, Color color) { this.size = size; this.color = color; }
        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }
        public void paintIcon(Component c, Graphics g, int x, int y) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(color); Path2D p = new Path2D.Double(); p.moveTo(x, y); p.lineTo(x+size, y+size/2.0); p.lineTo(x, y+size); p.lineTo(x+4, y+size/2.0); p.closePath(); g2.fill(p); g2.dispose(); }
    }
    private static class GifIcon implements Icon {
        private final int w, h; private final Color color;
        public GifIcon(int w, int h, Color color) { this.w = w; this.h = h; this.color = color; }
        public int getIconWidth() { return w; }
        public int getIconHeight() { return h; }
        public void paintIcon(Component c, Graphics g, int x, int y) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(color); g2.setStroke(new BasicStroke(1.5f)); g2.drawRoundRect(x, y+(h-14)/2+2, w, 14, 4, 4); g2.setFont(new Font("SansSerif", Font.BOLD, 9)); g2.drawString("GIF", x+(w-15)/2, y+(h-10)/2+10); g2.dispose(); }
    }
}