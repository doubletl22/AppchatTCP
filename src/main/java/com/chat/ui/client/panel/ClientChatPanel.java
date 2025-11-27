package com.chat.ui.client.panel;

import com.chat.model.ClientViewModel;
import com.chat.model.Message;
import com.chat.ui.client.ClientController;
import com.chat.ui.client.dialog.GifPickerDialog;
import com.chat.util.AudioUtils;
import com.chat.util.UiUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.io.File;

public class ClientChatPanel extends JPanel {

    private final JPanel chatDisplayPanel = new JPanel(new GridBagLayout());

    // [SỬA] Dùng lớp con RoundedTextField tự tạo bên dưới để vẽ đúng thứ tự
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
        chatScroll.setBorder(BorderFactory.createEmptyBorder());
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

        // Logic Mic
        micBtn.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { micBtn.setIcon(new MicIcon(22, Color.RED)); }
            public void mouseReleased(MouseEvent e) {
                micBtn.setIcon(new MicIcon(22, UiUtils.TEAL_COLOR));
                new Thread(() -> {
                    String base64 = audioRecorder.stopRecording();
                    if (base64 != null && controller != null) controller.handleSendVoice(base64);
                }).start();
            }
        });
        micBtn.addActionListener(e -> { try { audioRecorder.startRecording(); } catch(Exception ex){} });

        imageBtn.addActionListener(e -> chooseAndSendImage());
        gifBtn.addActionListener(e -> showGifPicker());
        stickerBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Tính năng Sticker đang phát triển!"));

        gbc.gridx = 0; bottomInput.add(micBtn, gbc);
        gbc.gridx = 1; bottomInput.add(imageBtn, gbc);
        gbc.gridx = 2; bottomInput.add(stickerBtn, gbc);
        gbc.gridx = 3; bottomInput.add(gifBtn, gbc);

        // --- B. Ô NHẬP LIỆU (ĐÃ SỬA LỖI MẤT CHỮ) ---
        inputField.setAction(sendAction);
        inputField.putClientProperty("JTextField.placeholderText", "Nhập tin nhắn...");
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Thêm padding cho chữ không dính mép
        inputField.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 10));

        // Nút Emoji bên trong ô nhập
        setupIconButton(emojiBtn, new EmojiIcon(20, UiUtils.TEAL_COLOR));
        emojiBtn.addActionListener(e -> showEmojiPopup(emojiBtn));
        inputField.putClientProperty("JTextField.trailingComponent", emojiBtn);

        gbc.gridx = 4;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        bottomInput.add(inputField, gbc);

        // --- C. NÚT GỬI ---
        sendBtn.setAction(sendAction);
        setupIconButton(sendBtn, new SendIcon(24, UiUtils.TEAL_COLOR));

        gbc.gridx = 5;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        bottomInput.add(sendBtn, gbc);

        add(bottomInput, BorderLayout.SOUTH);

        updateInputStyle();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (inputField != null) updateInputStyle();
    }

    private void updateInputStyle() {
        inputField.setForeground(UIManager.getColor("TextField.foreground"));
        inputField.setCaretColor(UIManager.getColor("TextField.caretForeground"));
        // Màu nền được xử lý trong paintComponent của RoundedTextField
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

    // --- CÁC HÀM LOGIC (Giữ nguyên) ---
    public void setController(ClientController controller) { this.controller = controller; }
    public String getInputText() { return inputField.getText(); }
    public void clearInputField() { inputField.setText(""); }
    public void clearChatDisplay() {
        UiUtils.invokeLater(() -> {
            chatDisplayPanel.removeAll();
            chatDisplayPanel.revalidate();
            chatDisplayPanel.repaint();
        });
    }
    private void chooseAndSendImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn ảnh để gửi");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Hình ảnh (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif");
        fileChooser.setFileFilter(filter);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            if (controller != null) controller.handleSendImage(fileChooser.getSelectedFile());
        }
    }
    private void showGifPicker() {
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        GifPickerDialog dialog = new GifPickerDialog(parentFrame, (selectedUrl) -> {
            inputField.setText("/gif " + selectedUrl);
            if (controller != null) controller.handleSend(inputField.getText());
            clearInputField();
        });
        dialog.setVisible(true);
    }
    private void showEmojiPopup(Component invoker) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(UIManager.getColor("Panel.background"));
        popup.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        String[] emojis = {"😀", "😂", "🥰", "😎", "😭", "👍", "👎", "❤️", "🔥", "🎉"};
        JPanel panel = new JPanel(new GridLayout(2, 5, 5, 5));
        panel.setOpaque(false);
        for (String emoji : emojis) {
            JButton btn = new JButton(emoji);
            btn.setFont(btn.getFont().deriveFont(20f));
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.addActionListener(e -> {
                inputField.replaceSelection(emoji);
                popup.setVisible(false);
            });
            panel.add(btn);
        }
        popup.add(panel);
        popup.show(invoker, 0, -80);
    }
    public void appendMessage(Message m, String currentUserName) {
        boolean isVoice = "voice".equals(m.type) || "dm_voice".equals(m.type);
        boolean isGif = "gif".equals(m.type) || "dm_gif".equals(m.type) || "gif_history".equals(m.type) || "dm_gif_history".equals(m.type);
        boolean isImage = "image".equals(m.type) || "dm_image".equals(m.type);
        boolean isSelf;
        if (m.name != null && m.name.startsWith("[TO ")) isSelf = true;
        else isSelf = m.name != null && m.name.equals(currentUserName);

        UiUtils.invokeLater(() -> {
            if ("system".equals(m.type)) {
                JLabel systemLabel = UiUtils.createSystemMessageLabel(m.text);
                GridBagConstraints gbc = createGBC(GridBagConstraints.CENTER);
                chatDisplayPanel.add(systemLabel, gbc);
            } else {
                JPanel messageBubble;
                if (isVoice) messageBubble = createVoiceBubble(m.name, m.data, isSelf);
                else if (isGif) messageBubble = createGifBubble(m.name, m.text, isSelf);
                else if (isImage) messageBubble = createImageBubble(m.name, m.data, isSelf);
                else messageBubble = createChatBubble(m.name, m.text, isSelf);
                JPanel alignmentWrapper = new JPanel(new FlowLayout(isSelf ? FlowLayout.RIGHT : FlowLayout.LEFT, 10, 2));
                alignmentWrapper.setOpaque(false);
                alignmentWrapper.add(messageBubble);
                GridBagConstraints gbc = createGBC(isSelf ? GridBagConstraints.EAST : GridBagConstraints.WEST);
                chatDisplayPanel.add(alignmentWrapper, gbc);
            }
            updateFiller();
            chatDisplayPanel.revalidate();
            chatDisplayPanel.repaint();
            JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, chatDisplayPanel);
            if (scrollPane != null) {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            }
        });
    }
    private JPanel createChatBubble(String sender, String text, boolean isSelf) {
        JPanel bubblePanel = new JPanel();
        bubblePanel.setLayout(new BoxLayout(bubblePanel, BoxLayout.Y_AXIS));
        bubblePanel.putClientProperty("FlatPanel.arc", 20);
        bubblePanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        bubblePanel.setOpaque(true);
        bubblePanel.setBackground(isSelf ? UIManager.getColor("App.selfMessageBackground") : UIManager.getColor("App.otherMessageBackground"));
        if (!isSelf && sender != null && !sender.equals("Public Chat")) {
            JLabel senderLabel = new JLabel(sender);
            senderLabel.setFont(senderLabel.getFont().deriveFont(Font.BOLD, 10f));
            senderLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            senderLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
            bubblePanel.add(senderLabel);
        }
        JTextPane textPane = new JTextPane();
        textPane.setText(text);
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setBorder(null);
        textPane.setForeground(isSelf ? UIManager.getColor("App.selfMessageForeground") : UIManager.getColor("App.otherMessageForeground"));
        textPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textPane.setPreferredSize(new Dimension(Math.min(350, getFontMetrics(textPane.getFont()).stringWidth(text) + 20), textPane.getPreferredSize().height));
        textPane.setSize(new Dimension(350, Short.MAX_VALUE));
        bubblePanel.add(textPane);
        return bubblePanel;
    }
    private JPanel createVoiceBubble(String sender, String base64Audio, boolean isSelf) {
        JPanel bubblePanel = new JPanel();
        bubblePanel.setLayout(new BoxLayout(bubblePanel, BoxLayout.Y_AXIS));
        bubblePanel.putClientProperty("FlatPanel.arc", 20);
        bubblePanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        bubblePanel.setOpaque(true);
        bubblePanel.setBackground(isSelf ? UIManager.getColor("App.selfMessageBackground") : UIManager.getColor("App.otherMessageBackground"));
        if (!isSelf && sender != null && !sender.equals("Public Chat")) {
            JLabel senderLabel = new JLabel(sender);
            senderLabel.setFont(senderLabel.getFont().deriveFont(Font.BOLD, 10f));
            senderLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            bubblePanel.add(senderLabel);
        }
        String sizeText = "Voice Message";
        if (base64Audio != null) sizeText = (base64Audio.length() / 1024) + " KB";
        JButton playBtn = new JButton("▶ " + sizeText);
        playBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        playBtn.putClientProperty("JButton.buttonType", "roundRect");
        playBtn.setBackground(isSelf ? new Color(0,0,0,50) : Color.WHITE);
        playBtn.setForeground(isSelf ? Color.WHITE : Color.BLACK);
        playBtn.addActionListener(e -> { if (base64Audio != null) AudioUtils.playBase64Audio(base64Audio); });
        bubblePanel.add(playBtn);
        return bubblePanel;
    }
    private JPanel createGifBubble(String sender, String gifUrl, boolean isSelf) { return createChatBubble(sender, "[GIF]: " + gifUrl, isSelf); }
    private JPanel createImageBubble(String sender, String base64Data, boolean isSelf) { return createChatBubble(sender, "[Ảnh]", isSelf); }
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

    // =========================================================================================
    // --- LỚP TÙY CHỈNH VẼ GIAO DIỆN (NẰM DƯỚI CÙNG FILE) ---
    // =========================================================================================

    // 1. [SỬA LỖI] Lớp JTextField tùy chỉnh để vẽ nền TRƯỚC khi vẽ chữ
    private static class RoundedTextField extends JTextField {
        private final int radius;
        public RoundedTextField(int radius) {
            this.radius = radius;
            setOpaque(false); // Để ta tự vẽ nền
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 1. Vẽ nền (Background)
            Color bgColor = UIManager.getColor("TextField.background");
            // Fix màu nền cho Light Mode
            if (getParent() != null && getParent().getBackground().getRed() > 200) {
                bgColor = new Color(240, 242, 245);
            }
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);

            // 2. Vẽ viền (Border)
            g2.setColor(new Color(200, 200, 200));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);

            g2.dispose();

            // 3. Gọi super để vẽ chữ đè lên nền (QUAN TRỌNG)
            super.paintComponent(g);
        }
    }

    // 2. Icon Mic
    private static class MicIcon implements Icon {
        private final int size; private final Color color;
        public MicIcon(int size, Color color) { this.size = size; this.color = color; }
        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            int w = size/3 + 2; int h = size/2;
            g2.fillRoundRect(x + (size-w)/2, y, w, h, w, w);
            g2.setStroke(new BasicStroke(2));
            g2.drawArc(x + 2, y + 2, size - 5, size - 5, 180, 180);
            g2.drawLine(x + size/2, y + size - 3, x + size/2, y + size);
            g2.drawLine(x + size/3, y + size, x + size - size/3, y + size);
            g2.dispose();
        }
    }

    // 3. Icon Image
    private static class ImageIconShape implements Icon {
        private final int size; private final Color color;
        public ImageIconShape(int size, Color color) { this.size = size; this.color = color; }
        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(x, y + 2, size, size - 4, 4, 4);
            Path2D path = new Path2D.Double();
            path.moveTo(x + 2, y + size - 4);
            path.lineTo(x + size/3.0, y + size/2.0);
            path.lineTo(x + size/2.0, y + size - 4);
            path.lineTo(x + size/1.5, y + size/3.0);
            path.lineTo(x + size - 2, y + size - 4);
            g2.draw(path);
            g2.fillOval(x + size - 8, y + 5, 4, 4);
            g2.dispose();
        }
    }

    // 4. Icon Sticker
    private static class StickerIcon implements Icon {
        private final int size; private final Color color;
        public StickerIcon(int size, Color color) { this.size = size; this.color = color; }
        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(x, y, size - 1, size - 1);
            g2.fillOval(x + 5, y + 7, 3, 3);
            g2.fillOval(x + size - 8, y + 7, 3, 3);
            g2.drawArc(x + 4, y + 8, size - 9, size - 12, 0, -180);
            g2.dispose();
        }
    }

    // 5. Icon Emoji
    private static class EmojiIcon extends StickerIcon {
        public EmojiIcon(int size, Color color) { super(size, color); }
    }

    // 6. Icon Send
    private static class SendIcon implements Icon {
        private final int size; private final Color color;
        public SendIcon(int size, Color color) { this.size = size; this.color = color; }
        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            Path2D path = new Path2D.Double();
            path.moveTo(x, y);
            path.lineTo(x + size, y + size/2.0);
            path.lineTo(x, y + size);
            path.lineTo(x + 4, y + size/2.0);
            path.closePath();
            g2.fill(path);
            g2.dispose();
        }
    }

    // 7. Icon GIF
    private static class GifIcon implements Icon {
        private final int w; private final int h; private final Color color;
        public GifIcon(int w, int h, Color color) { this.w = w; this.h = h; this.color = color; }
        public int getIconWidth() { return w; }
        public int getIconHeight() { return h; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x, y + (h-14)/2 + 2, w, 14, 4, 4);
            g2.setFont(new Font("SansSerif", Font.BOLD, 9));
            FontMetrics fm = g2.getFontMetrics();
            String text = "GIF";
            int tx = x + (w - fm.stringWidth(text))/2;
            int ty = y + (h - fm.getHeight())/2 + fm.getAscent() + 3;
            g2.drawString(text, tx, ty);
            g2.dispose();
        }
    }
}