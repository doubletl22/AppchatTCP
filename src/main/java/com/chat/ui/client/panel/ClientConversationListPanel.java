package com.chat.ui.client.panel;

import com.chat.model.ClientViewModel;
import com.chat.util.UiUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

public class ClientConversationListPanel extends JPanel {
    private final JList<String> conversationList = new JList<>();
    private final ClientViewModel viewModel;

    public ClientConversationListPanel(ClientViewModel viewModel) {
        this.viewModel = viewModel;
        setLayout(new BorderLayout());
        // Không setBackground cứng
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(200, 200, 200)));

        // Header "Đoạn chat"
        JLabel title = new JLabel("Đoạn chat");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setBorder(new EmptyBorder(15, 15, 10, 15));
        add(title, BorderLayout.NORTH);

        conversationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        conversationList.setModel(viewModel.getConversationListModel());
        conversationList.setFixedCellHeight(70);
        conversationList.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane listScroll = new JScrollPane(conversationList);
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        listScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // --- RENDERER ĐỘNG (Dùng UIManager) ---
        conversationList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JPanel panel = new JPanel(new BorderLayout(15, 0)) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        if (isSelected) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                            // Màu nền khi chọn: Lấy từ Theme (thường là xanh dương nhạt hoặc xám đậm)
                            g2.setColor(UIManager.getColor("List.selectionBackground"));
                            // Vẽ bo tròn
                            g2.fill(new RoundRectangle2D.Double(5, 5, getWidth()-10, getHeight()-10, 15, 15));
                            g2.dispose();
                        }
                    }
                };
                panel.setOpaque(false);
                panel.setBorder(new EmptyBorder(5, 10, 5, 10));

                String name = value.toString();

                // Avatar
                JLabel avatarLabel = new JLabel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        // Vòng tròn avatar (Màu nền avatar trung tính)
                        g2.setColor(new Color(150, 150, 150));
                        g2.fill(new Ellipse2D.Double(0, 0, 45, 45));

                        // Chữ cái đầu
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
                        String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(letter, (45 - fm.stringWidth(letter)) / 2, (45 - fm.getAscent()) / 2 + fm.getAscent() - 2);
                        g2.dispose();
                    }
                    @Override
                    public Dimension getPreferredSize() { return new Dimension(45, 45); }
                };

                // Tên User
                JPanel textPanel = new JPanel(new GridLayout(2, 1));
                textPanel.setOpaque(false);

                JLabel nameLabel = new JLabel(name);
                nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));

                // Màu chữ tên: Trắng nếu đang chọn, ngược lại theo theme (Đen/Trắng)
                if (isSelected) {
                    nameLabel.setForeground(UIManager.getColor("List.selectionForeground"));
                } else {
                    nameLabel.setForeground(UIManager.getColor("List.foreground"));
                }

                JLabel subLabel = new JLabel(isSelected ? "Đang chat..." : "Click để xem");
                subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                subLabel.setForeground(isSelected ? UIManager.getColor("List.selectionForeground") : Color.GRAY);

                textPanel.add(nameLabel);
                textPanel.add(subLabel);

                panel.add(avatarLabel, BorderLayout.WEST);
                panel.add(textPanel, BorderLayout.CENTER);
                return panel;
            }
        });

        add(listScroll, BorderLayout.CENTER);

        if (viewModel.getConversationListModel().getSize() > 0) {
            conversationList.setSelectedIndex(0);
        }
    }

    public void setListModel(ListModel<String> model) { conversationList.setModel(model); }
    public void addListSelectionListener(ListSelectionListener listener) { conversationList.addListSelectionListener(listener); }
    public String getSelectedValue() { return conversationList.getSelectedValue(); }
    public void restoreSelection(String recipient) {
        ListModel<String> model = conversationList.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            if (model.getElementAt(i).equals(recipient)) {
                conversationList.setSelectedIndex(i);
                return;
            }
        }
        conversationList.setSelectedIndex(0);
    }
}