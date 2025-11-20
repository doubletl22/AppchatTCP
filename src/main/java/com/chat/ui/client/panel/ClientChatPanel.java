package com.chat.ui.client.panel;

import com.chat.model.ClientViewModel;
import com.chat.model.Message;
import com.chat.ui.client.ClientController; // Import Controller để gửi voice
import com.chat.ui.client.dialog.GifPickerDialog;
import com.chat.util.AudioUtils; // Import tiện ích xử lý âm thanh
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
    private final JButton emojiBtn = new JButton("😊");
    private final JButton gifBtn = new JButton("GIF");

    // [MỚI] Thêm nút Mic và bộ ghi âm
    private final JButton micBtn = new JButton("🎤");
    private final AudioUtils audioRecorder = new AudioUtils();

    private final ClientViewModel viewModel;

    // [MỚI] Biến Controller để gọi hàm gửi tin nhắn thoại
    private ClientController controller;

    public ClientChatPanel(ClientViewModel viewModel, Action sendAction) {
        this.viewModel = viewModel;
        setLayout(new BorderLayout(5, 5));

        // --- 1. Khu vực hiển thị chat (Chat Area) ---
        JScrollPane chatScroll = new JScrollPane(chatDisplayPanel);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);
        chatScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatScroll.setBorder(BorderFactory.createEmptyBorder());
        add(chatScroll, BorderLayout.CENTER);

        // --- 2. Khu vực nhập liệu (Input Panel) ---
        JPanel bottomInput = new JPanel(new BorderLayout(5, 5));
        bottomInput.setBorder(new EmptyBorder(5, 10, 10, 10));

        // Cấu hình nút Gửi
        sendBtn.setAction(sendAction);
        sendBtn.putClientProperty("JButton.buttonType", "roundRect"); // FlatLaf style
        sendBtn.setText("Gửi");
        sendBtn.setPreferredSize(new Dimension(80, 30));

        // Gắn Action cho ô nhập liệu (Enter để gửi)
        inputField.addActionListener(sendAction);
        inputField.setAction(sendAction);

        bottomInput.add(inputField, BorderLayout.CENTER);

        // --- 3. Thanh công cụ (Emoji, GIF, Mic, Send) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));

        // Cấu hình nút Emoji
        emojiBtn.setFont(emojiBtn.getFont().deriveFont(18f));
        emojiBtn.setPreferredSize(new Dimension(40, 30));
        emojiBtn.addActionListener(e -> showEmojiPopup(emojiBtn));

        // Cấu hình nút GIF
        gifBtn.setFont(gifBtn.getFont().deriveFont(10f));
        gifBtn.setPreferredSize(new Dimension(50, 30));
        gifBtn.addActionListener(e -> showGifPicker());

        // [MỚI] Cấu hình nút Mic (Ghi âm)
        micBtn.setFont(micBtn.getFont().deriveFont(14f));
        micBtn.setPreferredSize(new Dimension(50, 30));
        micBtn.setToolTipText("Giữ chuột để nói, thả ra để gửi");

        // Sự kiện nhấn giữ Mic
        micBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                try {
                    // Bắt đầu ghi âm
                    micBtn.setBackground(Color.RED);
                    micBtn.setForeground(Color.WHITE);
                    audioRecorder.startRecording();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ClientChatPanel.this, "Lỗi Mic: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Kết thúc ghi âm
                micBtn.setBackground(null);
                micBtn.setForeground(null);
                String base64Sound = audioRecorder.stopRecording();

                // Kiểm tra và gửi dữ liệu
                if (base64Sound != null && !base64Sound.isEmpty()) {
                    if (controller != null) {
                        controller.handleSendVoice(base64Sound);
                    } else {
                        System.err.println("Controller chưa được khởi tạo (null)!");
                    }
                }
            }
        });

        buttonPanel.add(emojiBtn);
        buttonPanel.add(gifBtn);
        buttonPanel.add(micBtn); // Thêm nút Mic vào giữa
        buttonPanel.add(sendBtn);

        bottomInput.add(buttonPanel, BorderLayout.EAST);
        add(bottomInput, BorderLayout.SOUTH);
    }

    // [MỚI] Hàm setter để Inject Controller từ bên ngoài (ClientView)
    public void setController(ClientController controller) {
        this.controller = controller;
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
    private void showGifPicker() {
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        GifPickerDialog dialog = new GifPickerDialog(parentFrame, (selectedUrl) -> {
            inputField.setText("/gif " + selectedUrl);
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

    // --- Logic Hiển thị tin nhắn (Text, GIF, Voice) ---
    public void appendMessage(Message m, String currentUserName) {
        // Kiểm tra loại tin nhắn
        boolean isVoice = "voice".equals(m.type) || "dm_voice".equals(m.type);
        boolean isGif = "gif".equals(m.type) || "dm_gif".equals(m.type) ||
                "gif_history".equals(m.type) || "dm_gif_history".equals(m.type);

        // Xác định người gửi là chính mình hay người khác
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

                if (isVoice) {
                    // [MỚI] Tạo bong bóng tin nhắn thoại
                    messageBubble = createVoiceBubble(m.name, m.data, isSelf);
                } else if (isGif) {
                    // Tạo bong bóng GIF
                    messageBubble = createGifBubble(m.name, m.text, isSelf);
                } else {
                    // Tạo bong bóng Text thường
                    messageBubble = createChatBubble(m.name, m.text, isSelf);
                }

                // Căn chỉnh trái/phải
                JPanel alignmentWrapper = new JPanel(new FlowLayout(isSelf ? FlowLayout.RIGHT : FlowLayout.LEFT, 10, 5));
                alignmentWrapper.setBackground(chatDisplayPanel.getBackground());
                alignmentWrapper.add(messageBubble);

                GridBagConstraints gbc = createGBC(isSelf ? GridBagConstraints.EAST : GridBagConstraints.WEST);
                chatDisplayPanel.add(alignmentWrapper, gbc);
            }

            updateFiller();
            chatDisplayPanel.revalidate();
            chatDisplayPanel.repaint();

            // Tự động cuộn xuống cuối
            JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, chatDisplayPanel);
            if (scrollPane != null) {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            }
        });
    }

    // [MỚI] Hàm tạo bong bóng Voice Chat
    private JPanel createVoiceBubble(String sender, String base64Audio, boolean isSelf) {
        JPanel bubblePanel = new JPanel();
        bubblePanel.setLayout(new BoxLayout(bubblePanel, BoxLayout.Y_AXIS));
        bubblePanel.putClientProperty("FlatPanel.arc", 16);
        bubblePanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        bubblePanel.setOpaque(true);

        Color bgColor = isSelf ? new Color(0, 137, 255) : new Color(230, 230, 230);
        bubblePanel.setBackground(bgColor);

        // Hiển thị tên người gửi
        if (!isSelf && sender != null && !sender.equals("Public Chat")) {
            JLabel senderLabel = new JLabel(sender);
            senderLabel.setFont(senderLabel.getFont().deriveFont(Font.BOLD, 10f));
            senderLabel.setForeground(UIManager.getColor("text.gray"));
            bubblePanel.add(senderLabel);
        }

        // Tính dung lượng hiển thị (ước lượng)
        String sizeText = "0KB";
        if (base64Audio != null) {
            int kb = base64Audio.length() / 1024; // Base64 length roughly maps to size
            sizeText = kb + " KB";
        }

        // Nút Play
        JButton playBtn = new JButton("▶ Voice Chat (" + sizeText + ")");
        playBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        playBtn.addActionListener(e -> {
            if (base64Audio != null) {
                AudioUtils.playBase64Audio(base64Audio);
            }
        });

        bubblePanel.add(playBtn);
        return bubblePanel;
    }

    private JPanel createGifBubble(String sender, String gifUrl, boolean isSelf) {
        JPanel bubblePanel = new JPanel();
        bubblePanel.setLayout(new BoxLayout(bubblePanel, BoxLayout.Y_AXIS));
        bubblePanel.putClientProperty("FlatPanel.arc", 16);
        bubblePanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        bubblePanel.setOpaque(true);

        Color bgColor = isSelf ? new Color(0, 137, 255) : new Color(230, 230, 230);
        bubblePanel.setBackground(bgColor);

        JLabel gifLabel = new JLabel("Loading GIF...", SwingConstants.CENTER);
        gifLabel.setPreferredSize(new Dimension(200, 150));

        new Thread(() -> {
            try {
                URL url = new URL(gifUrl);
                ImageIcon icon = new ImageIcon(url);
                SwingUtilities.invokeLater(() -> {
                    gifLabel.setText("");
                    gifLabel.setIcon(icon);
                    bubblePanel.revalidate();
                    bubblePanel.repaint();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    gifLabel.setText("❌ Lỗi ảnh");
                    gifLabel.setForeground(Color.RED);
                });
            }
        }).start();

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