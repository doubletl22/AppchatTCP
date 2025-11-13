package com.chat.ui.client.panel;

import com.chat.model.ClientViewModel;
import com.chat.model.Message;
import com.chat.util.UiUtils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientChatPanel extends JPanel {
    // THAY ĐỔI: Khởi tạo với một placeholder trung lập
    private final JLabel chatHeaderLabel = new JLabel("Tin nhắn", SwingConstants.CENTER);
    private final JPanel chatDisplayPanel = new JPanel(new GridBagLayout());
    private final JTextField inputField = new JTextField();
    private final JButton sendBtn = new JButton();

    private final ClientViewModel viewModel;

    public ClientChatPanel(ClientViewModel viewModel, Action sendAction) {
        this.viewModel = viewModel;
        setLayout(new BorderLayout(5, 5));

        chatHeaderLabel.setFont(chatHeaderLabel.getFont().deriveFont(Font.BOLD, 14f));
        add(chatHeaderLabel, BorderLayout.NORTH);

        // Chat Area Setup
        chatDisplayPanel.setBackground(Color.WHITE);
        JScrollPane chatScroll = new JScrollPane(chatDisplayPanel);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);
        chatScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(chatScroll, BorderLayout.CENTER);

        // Input Panel Setup
        JPanel bottomInput = new JPanel(new BorderLayout(5, 5));
        sendBtn.setAction(sendAction);

        // Gắn Action cho nút Gửi và phím ENTER trong inputField
        inputField.addActionListener(sendAction);
        inputField.setAction(sendAction); // Action gắn vào Field để kích hoạt khi Enter

        bottomInput.add(inputField, BorderLayout.CENTER);
        bottomInput.add(sendBtn, BorderLayout.EAST);
        add(bottomInput, BorderLayout.SOUTH);
    }

    // Hàm này được gọi bởi ClientView thông qua binding để cập nhật tên người nhận
    public void setHeaderText(String text) {
        chatHeaderLabel.setText(text);
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

    public void appendMessage(Message m, String currentUserName) {
        // Updated check for GIF messages
        boolean isGifMessage = "gif".equals(m.type) || "dm_gif".equals(m.type) || "gif_history".equals(m.type) || "dm_gif_history".equals(m.type);

        boolean isSelf;
        // Logic xác định isSelf: Nếu tin nhắn là xác nhận DM gửi đi (Local Echo), hoặc tên khớp với tên người dùng hiện tại
        if (m.name != null && m.name.startsWith("[TO ")) {
            isSelf = true;
        } else {
            isSelf = m.name != null && m.name.equals(currentUserName);
        }

        UiUtils.invokeLater(() -> {

            // 1. Chỉ tin nhắn hệ thống (type="system") là non-bubble
            if ("system".equals(m.type)) {
                JLabel systemLabel = UiUtils.createSystemMessageLabel(m.text);
                GridBagConstraints gbc = createGBC(GridBagConstraints.CENTER);
                chatDisplayPanel.add(systemLabel, gbc);
            } else {
                // 2. Chat/DM/History (Bong bóng chat/GIF)

                JPanel messageBubble;
                if (isGifMessage) {
                    messageBubble = createGifBubble(m.name, m.text, isSelf);
                } else {
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
            JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, chatDisplayPanel);
            if (scrollPane != null) {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            }
        });
    }

    private String formatSystemOrHistoryMessage(Message m) {
        // Do history và dm_history đã được chuyển sang bubble, chỉ cần trả về text cho system
        return m.text;
    }

    // NEW: Function to create a GIF bubble/component
    private JPanel createGifBubble(String sender, String gifKeyword, boolean isSelf) {
        JPanel bubblePanel = new JPanel();
        bubblePanel.setLayout(new BoxLayout(bubblePanel, BoxLayout.Y_AXIS));
        bubblePanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        bubblePanel.setOpaque(true);

        Color bgColor = isSelf ? new Color(0, 137, 255) : new Color(230, 230, 230);
        Color fgColor = isSelf ? Color.WHITE : Color.BLACK;

        bubblePanel.setBackground(bgColor);

        // --- Simulated GIF Display ---
        JLabel gifLabel;
        try {
            // Sử dụng một Icon mặc định của hệ thống làm placeholder cho GIF
            Icon gifIcon = UIManager.getIcon("OptionPane.informationIcon");
            gifLabel = new JLabel("GIF: " + gifKeyword, gifIcon, SwingConstants.CENTER);

        } catch (Exception e) {
            gifLabel = new JLabel("Không tải được GIF. Keyword: " + gifKeyword);
        }

        gifLabel.setForeground(fgColor);
        gifLabel.setFont(gifLabel.getFont().deriveFont(Font.BOLD, 12f));
        gifLabel.setBorder(null);
        gifLabel.setPreferredSize(new Dimension(200, 100)); // Kích thước cố định cho placeholder
        gifLabel.setMaximumSize(new Dimension(300, 300));

        bubblePanel.add(gifLabel);
        // --- End Simulated GIF Display ---

        return bubblePanel;
    }

    private JPanel createChatBubble(String sender, String text, boolean isSelf) {
        JPanel bubblePanel = new JPanel();
        bubblePanel.setLayout(new BoxLayout(bubblePanel, BoxLayout.Y_AXIS));
        bubblePanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        bubblePanel.setOpaque(true);

        Color bgColor = isSelf ? new Color(0, 137, 255) : new Color(230, 230, 230);
        Color fgColor = isSelf ? Color.WHITE : Color.BLACK;

        bubblePanel.setBackground(bgColor);

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

        Component verticalGlue = null;//
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