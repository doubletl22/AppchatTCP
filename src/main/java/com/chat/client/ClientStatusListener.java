package com.chat.client;

import com.chat.Message;
import java.util.List;

public interface ClientStatusListener {
    void onConnectSuccess(String userName);
    void onDisconnect(String reason);
    void onMessageReceived(Message m);
    void onSystemMessage(String text);
    void onUserListUpdate(List<String> userNames, String selfName);
    void onAuthFailure(String reason);
}