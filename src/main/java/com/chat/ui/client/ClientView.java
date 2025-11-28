package com.chat.ui.client;

import com.chat.model.ClientViewModel;
import com.chat.ui.client.action.ConnectAction;
import com.chat.ui.client.action.DisconnectAction;
import com.chat.ui.client.action.SendAction;
import com.chat.ui.client.panel.ClientChatPanel;
import com.chat.ui.client.panel.ClientConnectPanel;
import com.chat.ui.client.panel.ClientConversationListPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ClientView extends JFrame {

    private ClientController controller;
    private final ClientViewModel viewModel;

    private ClientConnectPanel connectPanel;
    private ClientConversationListPanel conversationListPanel;
    private ClientChatPanel chatPanel;

    private final Action connectAction;
    private final Action disconnectAction;
    private final Action sendAction;

    public ClientView(ClientViewModel viewModel) {
        super("Chat Client");
        this.viewModel = viewModel;

        // Khởi tạo các Action (Controller sẽ được gán sau qua setController)
        this.connectAction = new ConnectAction(null);
        this.disconnectAction = new DisconnectAction(null);
        this.sendAction = new SendAction(null, null);

        // Khởi tạo các Panel
        this.connectPanel = new ClientConnectPanel(viewModel, connectAction, disconnectAction);
        this.chatPanel = new ClientChatPanel(viewModel, sendAction);
        this.conversationListPanel = new ClientConversationListPanel(viewModel);

        setupFrame();
        layoutComponents();

        bindModel();

        addRecipientChangeListener();
    }

    /**
     * Phương thức này được gọi từ AppLauncher để tiêm (inject) Controller vào View.
     * Điều này giúp phá vỡ vòng lặp dependency giữa View và Controller.
     */
    public void setController(ClientController controller) {
        this.controller = controller;

        // 1. Gán Controller cho các Actions (để nút bấm hoạt động)
        ((ConnectAction) connectAction).controller = controller;
        ((DisconnectAction) disconnectAction).controller = controller;
        ((SendAction) sendAction).controller = controller;
        ((SendAction) sendAction).chatPanel = chatPanel;

        // 2. [QUAN TRỌNG] Truyền Controller vào ChatPanel
        // Bước này bắt buộc để nút Voice (Mic) có thể gọi hàm handleSendVoice()
        if (chatPanel != null) {
            chatPanel.setController(controller);
        }
    }

    private void setupFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);
    }

    private void layoutComponents() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(connectPanel, BorderLayout.NORTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(250); // Độ rộng danh sách chat
        mainSplit.setBorder(new EmptyBorder(10, 10, 10, 10));

        mainSplit.setLeftComponent(conversationListPanel);
        mainSplit.setRightComponent(chatPanel);

        root.add(mainSplit, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void bindModel() {
        // Bind Conversation List (Danh sách người dùng)
        viewModel.onConversationListUpdate(() -> {
            conversationListPanel.setListModel(viewModel.getConversationListModel());
            conversationListPanel.restoreSelection(viewModel.getCurrentRecipient());
        });

        // Bind Chat Area (Nhận tin nhắn và hiển thị)
        viewModel.onMessage(m -> chatPanel.appendMessage(m, viewModel.getUserName()));
    }

    private void addRecipientChangeListener() {
        conversationListPanel.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && controller != null) {
                String selected = conversationListPanel.getSelectedValue();
                String currentRecipient = viewModel.getCurrentRecipient();

                if (selected != null && !selected.equals(currentRecipient)) {
                    // Xóa màn hình chat cũ khi chuyển người
                    chatPanel.clearChatDisplay();
                    viewModel.setCurrentRecipient(selected);

                    // [FIX] Luôn gọi requestHistory dù là Public hay Private
                    // Logic phân loại đã được xử lý bên trong controller.requestHistory()
                    controller.requestHistory(selected);
                }
            }
        });
    }
}