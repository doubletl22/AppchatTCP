package com.chat.ui.client.panel;

import com.chat.model.ClientViewModel;
import com.chat.model.Message;
import com.chat.ui.client.dialog.GifPickerDialog; // Import dialog mới tạo
import com.chat.util.UiUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URL;

public class ClientChatPanel extends JPanel {

    private final JPanel chatDisplayPanel = new JPanel(new GridBagLayout());
    private final JTextField inputField = new JTextField();
    private final JButton sendBtn = new JButton();
    private final JButton emojiBtn = new JButton("😊");

    // [NEW] Thêm nút GIF
    private final JButton gifBtn = new JButton("GIF");

    private final ClientViewModel viewModel;

    public ClientChatPanel(ClientViewModel viewModel, Action sendAction) {
        this.viewModel = viewModel;
        setLayout(new BorderLayout(5, 5));

        // --- 1. Chat Area Setup ---
        // Sử dụng JScrollPane để cuộn tin nhắn
        JScrollPane chatScroll = new JScrollPane(chatDisplayPanel);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);
        chatScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatScroll.setBorder(BorderFactory.createEmptyBorder());
        add(chatScroll, BorderLayout.CENTER);

        // --- 2. Input Panel Setup ---
        JPanel bottomInput = new JPanel(new BorderLayout(5, 5));
        bottomInput.setBorder(new EmptyBorder(5, 10, 10, 10));

        // Cấu hình nút Gửi
        sendBtn.setAction(sendAction);
        sendBtn.putClientProperty("JButton.buttonType", "roundRect"); // FlatLaf style
        sendBtn.setText("Gửi");
        sendBtn.setPreferredSize(new Dimension(80, 30));

        // Gắn Action cho nút Gửi và Enter
        inputField.addActionListener(sendAction);
        inputField.setAction(sendAction);

        bottomInput.add(inputField, BorderLayout.CENTER);

        // --- 3. Button Panel (Emoji, GIF, Send) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));

        // Cấu hình nút Emoji
        emojiBtn.setFont(emojiBtn.getFont().deriveFont(18f));
        emojiBtn.setPreferredSize(new Dimension(40, 30));
        emojiBtn.addActionListener(e -> showEmojiPopup(emojiBtn));

        // [NEW] Cấu hình nút GIF
        gifBtn.setFont(gifBtn.getFont().deriveFont(10f));
        gifBtn.setPreferredSize(new Dimension(50, 30));
        // Sự kiện khi bấm nút GIF -> Mở Dialog
        gifBtn.addActionListener(e -> showGifPicker());

        buttonPanel.add(emojiBtn);
        buttonPanel.add(gifBtn); // Thêm nút GIF vào giữa
        buttonPanel.add(sendBtn);

        bottomInput.add(buttonPanel, BorderLayout.EAST);
        add(bottomInput, BorderLayout.SOUTH);
    }

    public String getInputText() {
        return inputField.getText();
    }

    public void clearInputField() {
        inputField.setText("");
    }

    public void clearChatDisplay() {
        UiUtils.invokeLater(() -> {
            chatDisplayPanel.removeAll();
            chatDisplayPanel.revalidate();
            chatDisplayPanel.repaint();
        });
    }

    // --- Logic GIF ---

    // Hàm mở Dialog chọn GIF
    private void showGifPicker() {
        // Lấy Frame cha để hiển thị Dialog
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);

        GifPickerDialog dialog = new GifPickerDialog(parentFrame, (selectedUrl) -> {
            // Khi người dùng chọn một ảnh GIF:
            // 1. Điền lệnh /gif + URL vào ô chat
            inputField.setText("/gif " + selectedUrl);
            // 2. Tự động bấm nút gửi
            sendBtn.doClick();
        });

        dialog.setVisible(true);
    }

    // --- Logic Emoji ---

    private void showEmojiPopup(Component invoker) {
        JPopupMenu popup = new JPopupMenu();
        String[] emojis = {"😀", "😂", "🥰", "😎", "😭", "👍", "👎", "❤️", "🔥", "🎉"};
        JPanel panel = new JPanel(new GridLayout(2, 5, 2, 2));

        for (String emoji : emojis) {
            JButton btn = createEmojiButton(emoji, popup);
            panel.add(btn);
        }
        popup.add(panel);
        popup.show(invoker, 0, invoker.getHeight());
    }

    private JButton createEmojiButton(String emoji, JPopupMenu popup) {
        JButton btn = new JButton(emoji);
        btn.setFont(btn.getFont().deriveFont(20f));
        btn.setToolTipText(emoji);
        btn.setFocusable(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            inputField.replaceSelection(emoji);
            inputField.requestFocusInWindow();
            popup.setVisible(false);
        });
        return btn;
    }

    // --- Logic Hiển thị tin nhắn ---

    public void appendMessage(Message m, String currentUserName) {
        boolean isGifMessage = "gif".equals(m.type) || "dm_gif".equals(m.type)
                || "gif_history".equals(m.type) || "dm_gif_history".equals(m.type);

        boolean isSelf;
        if (m.name != null && m.name.startsWith("[TO ")) {
            isSelf = true;
        } else {
            isSelf = m.name != null && m.name.equals(currentUserName);
        }

        UiUtils.invokeLater(() -> {
            if ("system".equals(m.type)) {
                JLabel systemLabel = UiUtils.createSystemMessageLabel(m.text);
                GridBagConstraints gbc = createGBC(GridBagConstraints.CENTER);
                chatDisplayPanel.add(systemLabel, gbc);
            } else {
                JPanel messageBubble;
                if (isGifMessage) {
                    // Nếu là GIF, gọi hàm tạo bubble GIF
                    messageBubble = createGifBubble(m.name, m.text, isSelf);
                } else {
                    // Nếu là Chat thường
                    messageBubble = createChatBubble(m.name, m.text, isSelf);
                }

                JPanel alignmentWrapper = new JPanel(new FlowLayout(isSelf ? FlowLayout.RIGHT : FlowLayout.LEFT, 10, 5));
                alignmentWrapper.setBackground(chatDisplayPanel.getBackground());
                alignmentWrapper.add(messageBubble);

                GridBagConstraints gbc = createGBC(isSelf ? GridBagConstraints.EAST : GridBagConstraints.WEST);
                chatDisplayPanel.add(alignmentWrapper, gbc);
            }

            updateFiller();
            chatDisplayPanel.revalidate();
            chatDisplayPanel.repaint();

            // Tự động cuộn xuống dưới
            JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, chatDisplayPanel);
            if (scrollPane != null) {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            }
        });
    }

    /**
     * [QUAN TRỌNG] Hàm hiển thị bong bóng GIF từ URL
     */
    private JPanel createGifBubble(String sender, String gifUrl, boolean isSelf) {
        JPanel bubblePanel = new JPanel();
        bubblePanel.setLayout(new BoxLayout(bubblePanel, BoxLayout.Y_AXIS));
        bubblePanel.putClientProperty("FlatPanel.arc", 16);
        bubblePanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        bubblePanel.setOpaque(true);

        Color bgColor = isSelf ? new Color(0, 137, 255) : new Color(230, 230, 230);
        bubblePanel.setBackground(bgColor);

        // Label chứa ảnh
        JLabel gifLabel = new JLabel("Loading GIF...", SwingConstants.CENTER);
        gifLabel.setPreferredSize(new Dimension(200, 150)); // Kích thước mặc định

        // Tải ảnh từ URL trong luồng riêng (Thread) để tránh đơ ứng dụng
        new Thread(() -> {
            try {
                URL url = new URL(gifUrl);
                ImageIcon icon = new ImageIcon(url); // Swing tự động xử lý animation của GIF

                SwingUtilities.invokeLater(() -> {
                    gifLabel.setText(""); // Xóa chữ Loading
                    gifLabel.setIcon(icon);
                    // Cập nhật lại giao diện sau khi ảnh load xong
                    bubblePanel.revalidate();
                    bubblePanel.repaint();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    gifLabel.setText("❌ Lỗi tải ảnh");
                    gifLabel.setForeground(Color.RED);
                });
            }
        }).start();

        // Hiển thị tên người gửi (nếu không phải là mình)
        if (!isSelf && sender != null && !sender.equals("Public Chat")) {
            JLabel senderLabel = new JLabel(sender);
            senderLabel.setFont(senderLabel.getFont().deriveFont(Font.BOLD, 10f));
            senderLabel.setForeground(UIManager.getColor("text.gray"));
            bubblePanel.add(senderLabel);
        }

        bubblePanel.add(gifLabel);
        return bubblePanel;
    }

    private JPanel createChatBubble(String sender, String text, boolean isSelf) {
        JPanel bubblePanel = new JPanel();
        bubblePanel.setLayout(new BoxLayout(bubblePanel, BoxLayout.Y_AXIS));
        bubblePanel.putClientProperty("FlatPanel.arc", 16);
        bubblePanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        bubblePanel.setOpaque(true);

        Color bgColor = isSelf ? UIManager.getColor("Component.accentColor") : UIManager.getColor("Panel.background");
        Color fgColor = isSelf ? Color.WHITE : UIManager.getColor("Label.foreground");

        bubblePanel.setBackground(bgColor);

        if (!isSelf && sender != null && !sender.equals("Public Chat")) {
            JLabel senderLabel = new JLabel(sender);
            senderLabel.setFont(senderLabel.getFont().deriveFont(Font.BOLD, 10f));
            senderLabel.setForeground(UIManager.getColor("text.gray"));
            bubblePanel.add(senderLabel);
        }

        JTextPane textPane = new JTextPane();
        textPane.setText(text);
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setBackground(null);
        textPane.setForeground(fgColor);
        textPane.setBorder(null);
        textPane.setFont(textPane.getFont().deriveFont(13f));

        // Giới hạn chiều rộng bong bóng chat
        textPane.setPreferredSize(new Dimension(300, textPane.getPreferredSize().height));
        textPane.setMaximumSize(new Dimension(300, Integer.MAX_VALUE));

        bubblePanel.add(textPane);
        return bubblePanel;
    }

    private GridBagConstraints createGBC(int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.anchor = anchor;
        gbc.insets = new Insets(1, 5, 1, 5);
        return gbc;
    }

    private void updateFiller() {
        GridBagConstraints fillerGBC = new GridBagConstraints();
        fillerGBC.gridwidth = GridBagConstraints.REMAINDER;
        fillerGBC.weighty = 1.0;
        fillerGBC.fill = GridBagConstraints.VERTICAL;

        Component verticalGlue = null;
        if (chatDisplayPanel.getComponentCount() > 0) {
            Component lastComponent = chatDisplayPanel.getComponent(chatDisplayPanel.getComponentCount() - 1);
            if (lastComponent instanceof Box.Filler) {
                verticalGlue = lastComponent;
                chatDisplayPanel.remove(verticalGlue);
            }
        }

        if (verticalGlue == null) {
            verticalGlue = Box.createVerticalGlue();
        }
        chatDisplayPanel.add(verticalGlue, fillerGBC);
    }
}