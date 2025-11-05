package com.chat.model;

import com.chat.util.UiUtils;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ClientViewModel {

    private String userName = "User";
    private boolean connected = false;
    private boolean authenticated = false;
    private String currentRecipient = "Public Chat";
    private final List<String> currentUsers = new CopyOnWriteArrayList<>();

    private Consumer<String> statusUpdateListener;
    private Runnable conversationListUpdateListener;
    private Consumer<String> recipientUpdateListener;
    private Consumer<Message> chatUpdateListener;

    public String getUserName() { return userName; }
    public boolean isAuthenticated() { return authenticated; }
    public boolean isConnected() { return connected; }
    public String getCurrentRecipient() { return currentRecipient; }

    public ListModel<String> getConversationListModel() {
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("Public Chat");
        currentUsers.stream()
                .filter(n -> !n.equals(userName))
                .sorted()
                .forEach(model::addElement);
        return model;
    }

    // --- State Mutations (Chỉ Controller được gọi) ---

    public void setConnectionStatus(boolean connected, boolean authenticated) {
        UiUtils.invokeLater(() -> {
            this.connected = connected;
            this.authenticated = authenticated;
            updateStatus();
        });
    }

    public void setUserName(String userName) {
        UiUtils.invokeLater(() -> {
            this.userName = userName;
            this.currentUsers.add(userName);
            updateStatus();
        });
    }

    public void setCurrentRecipient(String recipient) {
        UiUtils.invokeLater(() -> {
            this.currentRecipient = recipient;
            if (recipientUpdateListener != null) {
                recipientUpdateListener.accept(recipient);
            }
        });
    }

    public void updateUsers(List<String> newUsers) {
        UiUtils.invokeLater(() -> {
            currentUsers.clear();
            currentUsers.add(userName);
            newUsers.stream()
                    .filter(n -> !n.equals(userName))
                    .forEach(currentUsers::add);
            if (conversationListUpdateListener != null) {
                conversationListUpdateListener.run();
            }
        });
    }

    public void addUser(String name) {
        UiUtils.invokeLater(() -> {
            if (!currentUsers.contains(name)) {
                currentUsers.add(name);
                if (conversationListUpdateListener != null) {
                    conversationListUpdateListener.run();
                }
            }
        });
    }

    public void removeUser(String name) {
        UiUtils.invokeLater(() -> {
            currentUsers.remove(name);
            if (conversationListUpdateListener != null) {
                conversationListUpdateListener.run();
            }
        });
    }

    // --- Listener Registration ---

    public void onStatusUpdate(Consumer<String> listener) {
        this.statusUpdateListener = listener;
        updateStatus();
    }

    public void onConversationListUpdate(Runnable listener) {
        this.conversationListUpdateListener = listener;
    }

    public void onRecipientUpdate(Consumer<String> listener) {
        this.recipientUpdateListener = listener;
    }

    public void onMessage(Consumer<Message> listener) {
        this.chatUpdateListener = listener;
    }

    public void notifyMessageReceived(Message message) {
        if (chatUpdateListener != null) {
            UiUtils.invokeLater(() -> chatUpdateListener.accept(message));
        }
    }

    // Private helper
    private void updateStatus() {
        if (statusUpdateListener != null) {
            String status;
            if (authenticated) {
                status = "Status: Logged in as " + userName;
            } else if (connected) {
                status = "Status: Connecting...";
            } else {
                status = "Trạng thái: Ngắt kết nối";
            }
            statusUpdateListener.accept(status);
        }
    }
}