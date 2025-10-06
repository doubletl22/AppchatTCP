package com.chat.common;

import java.io.Serializable;

/**
 * Lớp này đại diện cho một đối tượng tin nhắn được gửi qua mạng.
 * Nó phải implement Serializable để có thể được chuyển đổi thành một dòng byte.
 * Sử dụng record cho ngắn gọn và bất biến (immutable).
 */
public record Message(String sender, String content) implements Serializable {
    // Java tự động tạo constructor, getters, equals(), hashCode(), và toString() cho record.
}
