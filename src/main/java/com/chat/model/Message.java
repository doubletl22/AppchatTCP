package com.chat.model;

import java.util.List;

public class Message {
    public String type;
    public String time;
    public String name; // sender
    public String text;
    public String targetName; // receiver for dm
    public String username;
    public String password;
    public List<String> users; // Dành cho userlist
    public boolean isSelf = false; // Dành cho hiển thị cục bộ (Local Echo)

    // [MỚI] Trường chứa dữ liệu âm thanh (Base64 string) hoặc hình ảnh
    public String data;

    public Message() {}

    // --- NETWORK SENDING Methods (Dùng bởi Core để gửi đi) ---

    public static Message chat(String text) {
        Message m = new Message();
        m.type = "chat";
        m.text = text;
        return m;
    }

    public static Message direct(String targetName, String text) {
        Message m = new Message();
        m.type = "dm";
        m.targetName = targetName;
        m.text = text;
        return m;
    }

    // GIF Message for sending
    public static Message gif(String text, String recipient) {
        Message m = new Message();
        m.type = "Public Chat".equals(recipient) ? "gif" : "dm_gif";
        m.text = text; // text holds the GIF URL/Keyword
        m.targetName = "Public Chat".equals(recipient) ? null : recipient;
        return m;
    }

    // Voice Message for sending
    public static Message voice(String base64Data, String recipient) {
        Message m = new Message();
        m.type = "Public Chat".equals(recipient) ? "voice" : "dm_voice";
        m.targetName = "Public Chat".equals(recipient) ? null : recipient;
        m.data = base64Data;
        m.text = "[Tin nhắn thoại]"; // Nội dung hiển thị thay thế nếu client không hỗ trợ voice
        return m;
    }

    // Image Message for sending
    public static Message image(String base64Data, String recipient) {
        Message m = new Message();
        m.type = "Public Chat".equals(recipient) ? "image" : "dm_image";
        m.targetName = "Public Chat".equals(recipient) ? null : recipient;
        m.data = base64Data;
        m.text = "[Hình ảnh]"; // Nội dung hiển thị thay thế
        return m;
    }

    // Sticker Message for sending
    public static Message sticker(String stickerPath, String recipient) {
        Message m = new Message();
        // Nếu chat chung thì type là "sticker", chat riêng là "dm_sticker"
        m.type = "Public Chat".equals(recipient) ? "sticker" : "dm_sticker";
        m.text = stickerPath; // Nội dung là đường dẫn file ảnh (VD: /stickers/Tuzki/1.png)
        m.targetName = "Public Chat".equals(recipient) ? null : recipient;
        return m;
    }

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

    public static Message getDirectHistory(String targetUser) {
        Message m = new Message();
        m.type = "get_dm_history";
        m.targetName = targetUser;
        return m;
    }

    // --- CORE RECEIVING/DISPLAY Methods (Dùng bởi Core và Controller/ViewModel) ---

    public static Message system(String text) {
        Message m = new Message();
        m.type = "system";
        m.text = text;
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

    public static Message userlist(List<String> users) {
        Message m = new Message();
        m.type = "user_list";
        m.users = users;
        return m;
    }

    // Public History (Cập nhật xử lý Sticker cũ)
    public static Message history(String name, String text) {
        Message m = new Message();
        m.type = "history";

        if (text != null) {
            if (text.startsWith("[GIF]:")) {
                m.type = "gif_history";
                m.text = text.substring("[GIF]:".length()).trim();
            }
            // [QUAN TRỌNG] Nhận diện lịch sử Sticker
            else if (text.startsWith("[STICKER]:")) {
                m.type = "sticker_history";
                m.text = text.substring("[STICKER]:".length()).trim();
            }
            else {
                m.text = text;
            }
        }

        m.name = name;
        return m;
    }

    // DM History (Cập nhật xử lý Sticker cũ)
    public static Message directHistory(String sender, String text, String timestamp) {
        Message m = new Message();
        m.type = "dm_history";
        m.name = sender;

        if (text != null) {
            if (text.startsWith("[GIF]:")) {
                m.type = "dm_gif_history";
                m.text = text.substring("[GIF]:".length()).trim();
            }
            // [QUAN TRỌNG] Nhận diện lịch sử DM Sticker
            else if (text.startsWith("[STICKER]:")) {
                m.type = "dm_sticker_history";
                m.text = text.substring("[STICKER]:".length()).trim();
            }
            else {
                m.text = text;
            }
        }

        m.time = timestamp;
        return m;
    }

    // --- LOCAL ECHO/CONTROLLER DISPLAY Methods ---

    // Public chat (Dùng cho Local Echo)
    public static Message chat(String name, String text) {
        Message m = new Message();

        if (text != null && text.startsWith("[GIF]:")) {
            m.type = "gif";
            m.text = text.substring("[GIF]:".length()).trim();
        } else {
            m.type = "chat";
            m.text = text;
        }

        m.name = name;
        return m;
    }

    // DM (Dùng cho Local Echo)
    public static Message dm(String name, String targetName, String text, boolean isSelf) {
        Message m = new Message();

        if (text != null && text.startsWith("[GIF]:")) {
            m.type = "dm_gif";
            m.text = text.substring("[GIF]:".length()).trim();
        } else {
            m.type = "dm";
            m.text = text;
        }

        m.name = name;
        m.targetName = targetName;
        m.isSelf = isSelf;
        return m;
    }
}