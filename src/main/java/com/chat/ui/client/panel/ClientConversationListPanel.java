package com.chat.ui.client.panel;

import com.chat.model.ClientViewModel;

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
        setBackground(UIManager.getColor("Panel.background"));

        conversationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        conversationList.setModel(viewModel.getConversationListModel());

        // Tăng chiều cao mỗi dòng lên 60px để chứa Avatar to và thoáng hơn
        conversationList.setFixedCellHeight(60);

        // Padding xung quanh danh sách
        setBorder(new EmptyBorder(10, 10, 10, 10));
        conversationList.setBackground(UIManager.getColor("Panel.background"));

        JScrollPane listScroll = new JScrollPane(conversationList);
        listScroll.setBorder(BorderFactory.createEmptyBorder()); // Xóa viền xấu xí

        // --- CUSTOM RENDERER CAO CẤP ---
        conversationList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                // Panel chứa nội dung của từng dòng
                JPanel panel = new JPanel(new BorderLayout(15, 0)) { // Khoảng cách giữa Avatar và Text là 15px
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        // Chỉ vẽ nền khi item được chọn
                        if (isSelected) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(UIManager.getColor("Component.accentColor"));

                            // Vẽ hình chữ nhật bo tròn nằm gọn bên trong (Padding trên dưới 4px)
                            g2.fill(new RoundRectangle2D.Double(0, 4, getWidth(), getHeight() - 8, 16, 16));
                            g2.dispose();
                        }
                    }
                };

                // Quan trọng: Set Opaque false để trong suốt, cho phép thấy background của JList
                panel.setOpaque(false);
                panel.setBorder(new EmptyBorder(5, 10, 5, 10)); // Padding nội dung bên trong

                // 1. Tạo Avatar tròn
                String name = value.toString();
                JLabel avatarLabel = new JLabel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        // Màu nền Avatar: Trắng nếu đang chọn, Xám đậm nếu không chọn
                        if (isSelected) g2.setColor(Color.WHITE);
                        else g2.setColor(new Color(80, 80, 80));

                        g2.fill(new Ellipse2D.Double(0, 0, 40, 40)); // Kích thước Avatar 40x40

                        // Vẽ chữ cái đầu tên
                        if (isSelected) g2.setColor(UIManager.getColor("Component.accentColor"));
                        else g2.setColor(Color.WHITE);

                        g2.setFont(getFont().deriveFont(Font.BOLD, 18f));
                        String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
                        FontMetrics fm = g2.getFontMetrics();
                        // Căn giữa chữ
                        g2.drawString(letter, (40 - fm.stringWidth(letter)) / 2, (40 - fm.getAscent()) / 2 + fm.getAscent() - 2);
                        g2.dispose();
                    }
                    @Override
                    public Dimension getPreferredSize() { return new Dimension(40, 40); }
                };

                // 2. Phần Text (Tên + Trạng thái phụ)
                JPanel textPanel = new JPanel(new GridLayout(2, 1));
                textPanel.setOpaque(false);

                JLabel nameLabel = new JLabel(name);
                nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 14f));
                nameLabel.setForeground(isSelected ? Color.WHITE : UIManager.getColor("Label.foreground"));

                // Dòng phụ (Subtitle) giả lập
                JLabel subLabel = new JLabel(isSelected ? "Đang hoạt động" : "Click để chat");
                subLabel.setFont(subLabel.getFont().deriveFont(Font.PLAIN, 11f));
                subLabel.setForeground(isSelected ? new Color(230, 230, 230) : Color.GRAY);

                textPanel.add(nameLabel);
                textPanel.add(subLabel);

                panel.add(avatarLabel, BorderLayout.WEST);
                panel.add(textPanel, BorderLayout.CENTER);

                return panel;
            }
        });

        add(listScroll, BorderLayout.CENTER);

        // Mặc định chọn dòng đầu
        if (viewModel.getConversationListModel().getSize() > 0) {
            conversationList.setSelectedIndex(0);
        }
    }

    public void setListModel(ListModel<String> model) {
        conversationList.setModel(model);
    }

    public void addListSelectionListener(ListSelectionListener listener) {
        conversationList.addListSelectionListener(listener);
    }

    public String getSelectedValue() {
        return conversationList.getSelectedValue();
    }

    public void restoreSelection(String recipient) {
        int index = -1;
        ListModel<String> model = conversationList.getModel();

        for (int i = 0; i < model.getSize(); i++) {
            if (model.getElementAt(i).equals(recipient)) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            conversationList.setSelectedIndex(index);
        } else {
            conversationList.setSelectedIndex(0);
        }
    }
}