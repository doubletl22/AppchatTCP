
package com.chat;

public class Message {
    public String type; // "hello", "chat", "system", "kick"
    public String name;
    public String text;

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
}
