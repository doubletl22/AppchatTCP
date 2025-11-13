package com.chat.ui.client.panel;

import com.chat.model.ClientViewModel;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionListener;
import java.awt.*;

public class ClientConversationListPanel extends JPanel {
    private final JList<String> conversationList = new JList<>();
    private final ClientViewModel viewModel;

    public ClientConversationListPanel(ClientViewModel viewModel) {
        this.viewModel = viewModel;
        setLayout(new BorderLayout());

        conversationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Khởi tạo ListModel từ ViewModel
        conversationList.setModel(viewModel.getConversationListModel());
        conversationList.setSelectedIndex(0);

        // ĐÃ XÓA: Tiêu đề "Danh sách"
        // add(new JLabel("Danh sách", SwingConstants.CENTER), BorderLayout.NORTH);

        // THÊM: Border cho Panel để tách List khỏi mép JSplitPane
        setBorder(new EmptyBorder(0, 5, 0, 0));

        JScrollPane listScroll = new JScrollPane(conversationList);
        listScroll.setBorder(BorderFactory.createEmptyBorder()); // Xóa border của ScrollPane

        // THÊM: Custom Cell Renderer để thêm Padding cho từng Item
        conversationList.setCellRenderer(new DefaultListCellRenderer() {
            // Padding 8px trên/dưới, 10px trái/phải
            private final Border padding = BorderFactory.createEmptyBorder(8, 10, 8, 10);

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                // Lấy component mặc định (JLabel)
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                // Áp dụng padding
                label.setBorder(padding);

                // Đặt màu nền Transparent để List không bị đổi màu nền nếu không được chọn
                if (!isSelected) {
                    label.setBackground(new Color(0, 0, 0, 0));
                }

                return label;
            }
        });

        add(listScroll, BorderLayout.CENTER);
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