package com.chat.core; // Đã đổi package

import com.chat.model.Message;
import java.util.List;

public interface ClientStatusListener {
    void onConnectSuccess(String userName);
    void onDisconnect(String reason);
    void onMessageReceived(Message m);
    void onSystemMessage(String text);
    void onUserListUpdate(List<String> userNames, String selfName);
    void onAuthFailure(String reason);
}