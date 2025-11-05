package com.chat.core;

import java.util.List;

public interface ServerLogListener {
    void log(String message);
    void refreshClientList(List<String> clientNames);
    void showErrorMessage(String message);
}