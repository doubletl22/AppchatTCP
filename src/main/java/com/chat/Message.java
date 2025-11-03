package com.chat;

public class Message {
    // NEW types: "register", "login", "auth_success", "auth_failure", "history"
    public String type; // "hello", "chat", "system", "kick", "register", "login", "auth_success", "auth_failure", "history"
    public String name;
    public String text;
    public String username; // For login/register
    public String password; // For login/register

    public Message() {}

    public static Message hello(String name) {
        Message m = new Message();
        m.type = "hello";
        m.name = name;
        return m;
    }

    public static Message chat(String text) {
        Message m = new Message();
        m.type = "chat";
        m.text = text;
        return m;
    }

    public static Message system(String text) {
        Message m = new Message();
        m.type = "system";
        m.text = text;
        return m;
    }

    // NEW: Authentication Messages
    public static Message register(String username, String password) {
        Message m = new Message();
        m.type = "register";
        m.username = username;
        m.password = password;
        return m;
    }

    public static Message login(String username, String password) {
        Message m = new Message();
        m.type = "login";
        m.username = username;
        m.password = password;
        return m;
    }

    public static Message authSuccess(String name) {
        Message m = new Message();
        m.type = "auth_success";
        m.name = name;
        return m;
    }

    public static Message authFailure(String reason) {
        Message m = new Message();
        m.type = "auth_failure";
        m.text = reason;
        return m;
    }

    // NEW: History Message (server sends this)
    public static Message history(String name, String text) {
        Message m = new Message();
        m.type = "history";
        m.name = name;
        m.text = text;
        return m;
    }
}