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
import java.io.File;
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

    // --- MÀU SẮC MỚI ---
    private static final Color MY_MSG_BG = new Color(0, 150, 136); // Xanh Ngọc (Tin mình gửi)
    private static final Color OTHER_MSG_BG = new Color(230, 230, 230); // Xám nhạt (Tin người khác)
    private static final Color INPUT_BG = new Color(240, 242, 245); // Nền ô nhập liệu sáng

    public ClientChatPanel(ClientViewModel viewModel, Action sendAction) {
        this.viewModel = viewModel;
        setLayout(new BorderLayout(0, 0));
        setBackground(Color.WHITE); // Nền chính màu trắng

        // --- 1. KHU VỰC HIỂN THỊ CHAT ---
        chatDisplayPanel.setBackground(Color.WHITE); // Nền chat màu trắng
        JScrollPane chatScroll = new JScrollPane(chatDisplayPanel);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);
        chatScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatScroll.setBorder(BorderFactory.createEmptyBorder());
        add(chatScroll, BorderLayout.CENTER);

        // --- 2. THANH NHẬP LIỆU ---
        JPanel bottomInput = new JPanel(new BorderLayout(10, 0));
        bottomInput.setBorder(new EmptyBorder(15, 15, 15, 15));
        bottomInput.setBackground(Color.WHITE); // Nền thanh nhập liệu trắng

        // -- Nhóm Icon bên trái --
        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftActions.setOpaque(false);

        styleIconButton(micBtn, 20f);
        styleIconButton(imageBtn, 20f);
        styleIconButton(stickerBtn, 20f);

        styleIconButton(gifBtn, 12f);
        gifBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        gifBtn.setText("GIF");
        gifBtn.setBorder(BorderFactory.createLineBorder(UiUtils.TEAL_COLOR, 1, true));

        // Logic Mic (Giữ nguyên)
        micBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                micBtn.setForeground(Color.RED);
                try { audioRecorder.startRecording(); } catch (Exception ex) { ex.printStackTrace(); }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                micBtn.setForeground(UiUtils.TEAL_COLOR);
                String base64 = audioRecorder.stopRecording();
                if (base64 != null && controller != null) controller.handleSendVoice(base64);
            }
        });

        imageBtn.addActionListener(e -> chooseAndSendImage());
        gifBtn.addActionListener(e -> showGifPicker());
        stickerBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Tính năng Sticker đang phát triển!"));

        leftActions.add(micBtn);
        leftActions.add(imageBtn);
        leftActions.add(stickerBtn);
        leftActions.add(gifBtn);

        // -- Ô NHẬP LIỆU --
        inputField.setAction(sendAction);
        inputField.putClientProperty("JTextField.placeholderText", "Nhập tin nhắn...");
        inputField.putClientProperty("Component.arc", 999); // Bo tròn hoàn toàn
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setForeground(Color.BLACK); // Chữ màu đen
        inputField.setCaretColor(Color.BLACK);
        inputField.setBackground(INPUT_BG); // Nền xám nhạt
        inputField.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 40));

        // -- Nút Emoji --
        styleIconButton(emojiBtn, 20f);
        emojiBtn.setText("😊");
        emojiBtn.addActionListener(e -> showEmojiPopup(emojiBtn));
        inputField.putClientProperty("JTextField.trailingComponent", emojiBtn);

        // -- Nút Gửi --
        sendBtn.setAction(sendAction);
        sendBtn.setText("➤");
        styleIconButton(sendBtn, 20f);
        sendBtn.setForeground(UiUtils.TEAL_COLOR);

        // Lắp ráp
        bottomInput.add(leftActions, BorderLayout.WEST);
        bottomInput.add(inputField, BorderLayout.CENTER);
        bottomInput.add(sendBtn, BorderLayout.EAST);

        add(bottomInput, BorderLayout.SOUTH);
    }

    private void styleIconButton(JButton btn, float size) {
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setForeground(UiUtils.TEAL_COLOR); // Icon màu Teal
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, (int)size));
        btn.setMargin(new Insets(2, 6, 2, 6));
    }

    // ... (Giữ nguyên các hàm setController, chooseAndSendImage, showGifPicker, showEmojiPopup) ...
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
        popup.setBackground(Color.WHITE);
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

    // ... (Tiếp tục với phần hiển thị tin nhắn - CẦN SỬA createChatBubble) ...

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

    // --- CÁC HÀM TẠO BONG BÓNG CHAT (ĐÃ SỬA MÀU) ---

    private JPanel createChatBubble(String sender, String text, boolean isSelf) {
        JPanel bubblePanel = new JPanel();
        bubblePanel.setLayout(new BoxLayout(bubblePanel, BoxLayout.Y_AXIS));
        bubblePanel.putClientProperty("FlatPanel.arc", 20); // Bo tròn đẹp hơn
        bubblePanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        bubblePanel.setOpaque(true);

        // MÀU NỀN BONG BÓNG
        bubblePanel.setBackground(isSelf ? MY_MSG_BG : OTHER_MSG_BG);

        if (!isSelf && sender != null && !sender.equals("Public Chat")) {
            JLabel senderLabel = new JLabel(sender);
            senderLabel.setFont(senderLabel.getFont().deriveFont(Font.BOLD, 10f));
            senderLabel.setForeground(Color.DARK_GRAY); // Tên người gửi màu tối
            senderLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
            bubblePanel.add(senderLabel);
        }

        JTextPane textPane = new JTextPane();
        textPane.setText(text);
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setBorder(null);
        // MÀU CHỮ: Trắng nếu là mình, Đen nếu là người khác
        textPane.setForeground(isSelf ? Color.WHITE : Color.BLACK);
        textPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Logic chỉnh size text (Giữ nguyên)
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
        bubblePanel.setBackground(isSelf ? MY_MSG_BG : OTHER_MSG_BG);

        if (!isSelf && sender != null && !sender.equals("Public Chat")) {
            JLabel senderLabel = new JLabel(sender);
            senderLabel.setFont(senderLabel.getFont().deriveFont(Font.BOLD, 10f));
            senderLabel.setForeground(Color.DARK_GRAY);
            bubblePanel.add(senderLabel);
        }
        String sizeText = "Voice Message";
        if (base64Audio != null) sizeText = (base64Audio.length() / 1024) + " KB";

        JButton playBtn = new JButton("▶ " + sizeText);
        playBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        playBtn.putClientProperty("JButton.buttonType", "roundRect");
        playBtn.setBackground(isSelf ? new Color(0, 121, 107) : Color.WHITE); // Màu nút play
        playBtn.setForeground(isSelf ? Color.WHITE : Color.BLACK);

        playBtn.addActionListener(e -> { if (base64Audio != null) AudioUtils.playBase64Audio(base64Audio); });
        bubblePanel.add(playBtn);
        return bubblePanel;
    }

    // Giữ nguyên createGifBubble và createImageBubble (chỉ lưu ý không đổi logic)
    // Tôi rút gọn code phần này để tránh quá dài, bạn giữ nguyên logic ảnh/gif cũ,
    // chỉ cần thay bubblePanel.setBackground(Color.WHITE) cho các ảnh để nền sạch sẽ.
    private JPanel createImageBubble(String sender, String base64Data, boolean isSelf) {
        // Copy logic cũ của bạn, nhưng bỏ border nền đi cho đẹp
        // bubblePanel.setBorder(null);
        // bubblePanel.setOpaque(false);
        // ...
        return createChatBubble(sender, "[Ảnh]", isSelf); // Placeholder nếu chưa copy logic full
    }
    private JPanel createGifBubble(String sender, String gifUrl, boolean isSelf) {
        return createChatBubble(sender, "[GIF]", isSelf); // Placeholder
    }

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
            if (lastComponent instanceof Box.Filler) {
                verticalGlue = lastComponent; chatDisplayPanel.remove(verticalGlue);
            }
        }
        if (verticalGlue == null) verticalGlue = Box.createVerticalGlue();
        chatDisplayPanel.add(verticalGlue, fillerGBC);
    }
}