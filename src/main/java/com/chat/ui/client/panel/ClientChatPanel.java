package com.chat.ui.client.panel;

import com.chat.model.ClientViewModel;
import com.chat.model.Message;
import com.chat.ui.client.ClientController;
import com.chat.ui.client.dialog.GifPickerDialog;
import com.chat.util.AudioUtils;
import com.chat.util.UiUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

public class ClientChatPanel extends JPanel {

    private final JPanel chatDisplayPanel = new JPanel(new GridBagLayout());
    private final JTextField inputField = new JTextField();
    private final JButton sendBtn = new JButton();

    // Các nút chức năng
    private final JButton micBtn = new JButton("🎙");
    private final JButton imageBtn = new JButton("🖼");
    private final JButton stickerBtn = new JButton("☺");
    private final JButton gifBtn = new JButton("GIF");
    private final JButton emojiBtn = new JButton("😊");

    private final AudioUtils audioRecorder = new AudioUtils();
    private final ClientViewModel viewModel;
    private ClientController controller;

    // Màu xanh Messenger
    private static final Color MSG_BLUE = new Color(0, 132, 255);
    private static final Color INPUT_BG = new Color(58, 59, 60);

    public ClientChatPanel(ClientViewModel viewModel, Action sendAction) {
        this.viewModel = viewModel;
        setLayout(new BorderLayout(0, 0));
        setBackground(UIManager.getColor("Panel.background"));

        // --- 1. KHU VỰC HIỂN THỊ CHAT ---
        chatDisplayPanel.setBackground(UIManager.getColor("Panel.background"));
        JScrollPane chatScroll = new JScrollPane(chatDisplayPanel);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);
        chatScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatScroll.setBorder(BorderFactory.createEmptyBorder());
        add(chatScroll, BorderLayout.CENTER);

        // --- 2. THANH NHẬP LIỆU ---
        JPanel bottomInput = new JPanel(new BorderLayout(10, 0));
        bottomInput.setBorder(new EmptyBorder(10, 10, 10, 10));
        bottomInput.setBackground(UIManager.getColor("Panel.background"));

        // -- Nhóm Icon bên trái --
        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        leftActions.setOpaque(false);

        styleIconButton(micBtn, 20f);
        styleIconButton(imageBtn, 20f);
        styleIconButton(stickerBtn, 20f);

        // Nút GIF
        styleIconButton(gifBtn, 12f);
        gifBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gifBtn.setText("GIF");
        gifBtn.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // Logic Mic
        micBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                micBtn.setForeground(Color.RED);
                try { audioRecorder.startRecording(); } catch (Exception ex) { ex.printStackTrace(); }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                micBtn.setForeground(MSG_BLUE);
                String base64 = audioRecorder.stopRecording();
                if (base64 != null && controller != null) controller.handleSendVoice(base64);
            }
        });

        gifBtn.addActionListener(e -> showGifPicker());
        imageBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Tính năng gửi ảnh đang phát triển!"));
        stickerBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Tính năng Sticker đang phát triển!"));

        leftActions.add(micBtn);
        leftActions.add(imageBtn);
        leftActions.add(stickerBtn);
        leftActions.add(gifBtn);

        // -- Ô NHẬP LIỆU --
        inputField.setAction(sendAction);
        inputField.putClientProperty("JTextField.placeholderText", "Aa");
        inputField.putClientProperty("Component.arc", 999);
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(Color.WHITE);
        inputField.setBackground(INPUT_BG);
        inputField.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 40));

        // -- Nút Emoji --
        styleIconButton(emojiBtn, 20f);
        emojiBtn.setText("😊");
        emojiBtn.addActionListener(e -> showEmojiPopup(emojiBtn));
        inputField.putClientProperty("JTextField.trailingComponent", emojiBtn);

        // -- Nút Gửi --
        sendBtn.setAction(sendAction);
        sendBtn.setText("▶");
        styleIconButton(sendBtn, 20f);
        sendBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        sendBtn.setForeground(MSG_BLUE);

        // Lắp ráp
        bottomInput.add(leftActions, BorderLayout.WEST);
        bottomInput.add(inputField, BorderLayout.CENTER);
        bottomInput.add(sendBtn, BorderLayout.EAST);

        add(bottomInput, BorderLayout.SOUTH);
    }

    /** Helper style nút bấm */
    private void styleIconButton(JButton btn, float size) {
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setForeground(MSG_BLUE);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, (int)size));
        btn.setMargin(new Insets(2, 6, 2, 6));
    }

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
        popup.setBackground(new Color(40, 40, 40));
        popup.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        String[] emojis = {"😀", "😂", "🥰", "😎", "😭", "👍", "👎", "❤️", "🔥", "🎉"};
        JPanel panel = new JPanel(new GridLayout(2, 5, 5, 5));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        for (String emoji : emojis) {
            JButton btn = new JButton(emoji);
            btn.setFont(btn.getFont().deriveFont(24f));
            btn.setFocusable(false);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                inputField.replaceSelection(emoji);
                inputField.requestFocusInWindow();
                popup.setVisible(false);
            });
            panel.add(btn);
        }
        popup.add(panel);
        popup.show(invoker, -100, -popup.getPreferredSize().height - 10);
    }

    public void appendMessage(Message m, String currentUserName) {
        boolean isVoice = "voice".equals(m.type) || "dm_voice".equals(m.type);
        boolean isGif = "gif".equals(m.type) || "dm_gif".equals(m.type) ||
                "gif_history".equals(m.type) || "dm_gif_history".equals(m.type);
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

    private JPanel createVoiceBubble(String sender, String base64Audio, boolean isSelf) {
        JPanel bubblePanel = new JPanel();
        bubblePanel.setLayout(new BoxLayout(bubblePanel, BoxLayout.Y_AXIS));
        bubblePanel.putClientProperty("FlatPanel.arc", 18);
        bubblePanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        bubblePanel.setOpaque(true);
        bubblePanel.setBackground(isSelf ? UIManager.getColor("Component.accentColor") : new Color(60, 63, 65));
        if (!isSelf && sender != null && !sender.equals("Public Chat")) {
            JLabel senderLabel = new JLabel(sender);
            senderLabel.setFont(senderLabel.getFont().deriveFont(Font.BOLD, 10f));
            senderLabel.setForeground(new Color(180, 180, 180));
            bubblePanel.add(senderLabel);
        }
        String sizeText = "Voice";
        if (base64Audio != null) sizeText = (base64Audio.length() / 1024) + " KB";
        JButton playBtn = new JButton("▶ " + sizeText);
        playBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        playBtn.putClientProperty("JButton.buttonType", "roundRect");
        playBtn.addActionListener(e -> { if (base64Audio != null) AudioUtils.playBase64Audio(base64Audio); });
        bubblePanel.add(playBtn);
        return bubblePanel;
    }

    // [Bubble] GIF - ĐÃ SỬA ĐỂ LOẠI BỎ VIỀN BO TRÒN MÀU XANH/XÁM
    private JPanel createGifBubble(String sender, String gifUrl, boolean isSelf) {
        JPanel bubblePanel = new JPanel();
        bubblePanel.setLayout(new BoxLayout(bubblePanel, BoxLayout.Y_AXIS));

        // --- CÁC THAY ĐỔI ĐỂ BỎ VIỀN ---
        // 1. Bỏ property bo góc (FlatPanel.arc) vì không còn nền để bo.
        // bubblePanel.putClientProperty("FlatPanel.arc", 18); // Đã comment lại

        // 2. Đặt border là null để không có khoảng đệm, ảnh tràn ra mép.
        bubblePanel.setBorder(null);

        // 3. Đặt panel thành trong suốt (opaque = false).
        bubblePanel.setOpaque(false);

        // 4. Bỏ việc set màu nền (setBackground).
        // bubblePanel.setBackground(isSelf ? UIManager.getColor("Component.accentColor") : new Color(60, 63, 65)); // Đã comment lại
        // -------------------------------

        if (!isSelf && sender != null && !sender.equals("Public Chat")) {
            JLabel senderLabel = new JLabel(sender);
            senderLabel.setFont(senderLabel.getFont().deriveFont(Font.BOLD, 10f));
            senderLabel.setForeground(new Color(180, 180, 180));
            // Thêm chút padding nhẹ cho tên người gửi nếu cần
            senderLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
            bubblePanel.add(senderLabel);
        }
        JLabel loadingLabel = new JLabel("Loading...", SwingConstants.CENTER);
        loadingLabel.setPreferredSize(new Dimension(200, 150));
        loadingLabel.setForeground(Color.LIGHT_GRAY);
        new Thread(() -> {
            try {
                URL url = new URL(gifUrl);
                ImageIcon icon = new ImageIcon(url);
                while (icon.getImageLoadStatus() == MediaTracker.LOADING) Thread.sleep(50);
                int w = icon.getIconWidth(); int h = icon.getIconHeight();
                if (w <= 0 || h <= 0) { SwingUtilities.invokeLater(() -> loadingLabel.setText("❌ Lỗi ảnh")); return; }
                int maxWidth = 200; int maxHeight = 200;
                int newW = w; int newH = h;
                if (w > maxWidth || h > maxHeight) {
                    float ratio = (float) w / h;
                    if (ratio > 1) { newW = maxWidth; newH = (int) (maxWidth / ratio); }
                    else { newH = maxHeight; newW = (int) (maxHeight * ratio); }
                }
                int finalW = newW; int finalH = newH; Image img = icon.getImage();
                SwingUtilities.invokeLater(() -> {
                    bubblePanel.remove(loadingLabel);
                    JPanel imgPanel = new JPanel() {
                        @Override protected void paintComponent(Graphics g) {
                            super.paintComponent(g);
                            Graphics2D g2 = (Graphics2D) g;
                            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                            g2.drawImage(img, 0, 0, finalW, finalH, this);
                        }
                        @Override public Dimension getPreferredSize() { return new Dimension(finalW, finalH); }
                    };
                    imgPanel.setOpaque(false);
                    bubblePanel.add(imgPanel);
                    bubblePanel.revalidate(); bubblePanel.repaint();
                    if (chatDisplayPanel.getParent() != null) chatDisplayPanel.getParent().validate();
                });
            } catch (Exception e) { SwingUtilities.invokeLater(() -> loadingLabel.setText("❌ Lỗi URL")); }
        }).start();
        bubblePanel.add(loadingLabel);
        return bubblePanel;
    }

    private JPanel createChatBubble(String sender, String text, boolean isSelf) {
        JPanel bubblePanel = new JPanel();
        bubblePanel.setLayout(new BoxLayout(bubblePanel, BoxLayout.Y_AXIS));
        bubblePanel.putClientProperty("FlatPanel.arc", 18);
        bubblePanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        bubblePanel.setOpaque(true);
        bubblePanel.setBackground(isSelf ? UIManager.getColor("Component.accentColor") : new Color(60, 63, 65));
        if (!isSelf && sender != null && !sender.equals("Public Chat")) {
            JLabel senderLabel = new JLabel(sender);
            senderLabel.setFont(senderLabel.getFont().deriveFont(Font.BOLD, 11f));
            senderLabel.setForeground(new Color(200, 200, 200));
            senderLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
            bubblePanel.add(senderLabel);
        }
        JTextPane textPane = new JTextPane();
        textPane.setText(text); textPane.setEditable(false); textPane.setOpaque(false);
        textPane.setBackground(null); textPane.setBorder(null);
        textPane.setForeground(isSelf ? Color.WHITE : UIManager.getColor("Label.foreground"));
        textPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textPane.setPreferredSize(new Dimension(Math.min(300, getFontMetrics(textPane.getFont()).stringWidth(text) + 20), textPane.getPreferredSize().height));
        textPane.setSize(new Dimension(300, Short.MAX_VALUE));
        bubblePanel.add(textPane);
        return bubblePanel;
    }

    private GridBagConstraints createGBC(int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0; gbc.anchor = anchor; gbc.insets = new Insets(2, 5, 2, 5);
        return gbc;
    }

    private void updateFiller() {
        GridBagConstraints fillerGBC = new GridBagConstraints();
        fillerGBC.gridwidth = GridBagConstraints.REMAINDER; fillerGBC.weighty = 1.0;
        fillerGBC.fill = GridBagConstraints.VERTICAL;
        Component verticalGlue = null;
        if (chatDisplayPanel.getComponentCount() > 0) {
            Component lastComponent = chatDisplayPanel.getComponent(chatDisplayPanel.getComponentCount() - 1);
            if (lastComponent instanceof Box.Filler) {
                verticalGlue = lastComponent; chatDisplayPanel.remove(verticalGlue);
            }
        }
        if (verticalGlue == null) verticalGlue = Box.createVerticalGlue();
        chatDisplayPanel.add(verticalGlue, fillerGBC);
    }
}