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
    private final JLabel chatHeaderLabel = new JLabel("Public Chat", SwingConstants.CENTER);
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

        // Dòng lỗi đã bị xóa khỏi đây. Việc binding được thực hiện trong ClientView.java.
    }

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
        boolean isSelf = m.name != null && m.name.equals(currentUserName);

        UiUtils.invokeLater(() -> {
            // 1. Tin nhắn hệ thống, lịch sử chat công khai, lỗi
            if ("system".equals(m.type) || "history".equals(m.type) || "dm_history".equals(m.type)) {
                // Tin nhắn lịch sử/hệ thống đơn giản
                JLabel systemLabel = UiUtils.createSystemMessageLabel(formatSystemOrHistoryMessage(m));
                GridBagConstraints gbc = createGBC(GridBagConstraints.CENTER);
                chatDisplayPanel.add(systemLabel, gbc);
            } else {
                // 2. Chat/DM (Bong bóng chat)
                JPanel messageBubble = createChatBubble(m.name, m.text, isSelf);

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
        if ("history".equals(m.type)) {
            return "[HISTORY] " + m.name + ": " + m.text;
        } else if ("dm_history".equals(m.type)) {
            String prefix = m.name.equals(viewModel.getUserName()) ? "[Bạn] " : "[" + m.name + "] ";
            return prefix + m.text;
        }
        return m.text;
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