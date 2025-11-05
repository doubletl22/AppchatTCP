package com.chat.ui.client.panel;

import com.chat.model.ClientViewModel;

import javax.swing.*;
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

        add(new JLabel("Danh sách", SwingConstants.CENTER), BorderLayout.NORTH);
        add(new JScrollPane(conversationList), BorderLayout.CENTER);
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