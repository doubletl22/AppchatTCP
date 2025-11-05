package com.chat.service; // Đã đổi package

import com.chat.model.Message; // Đã đổi import
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:chat_database.db";

    public DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            createTables();
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found.");
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    private void createTables() {
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users (\n"
                + " username TEXT PRIMARY KEY,\n"
                + " password TEXT NOT NULL\n"
                + ");";
        String sqlMessages = "CREATE TABLE IF NOT EXISTS messages (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " sender TEXT NOT NULL,\n"
                + " text TEXT NOT NULL,\n"
                + " timestamp DATETIME DEFAULT CURRENT_TIMESTAMP\n"
                + ");";
        // Bảng mới cho tin nhắn trực tiếp
        String sqlDirectMessages = "CREATE TABLE IF NOT EXISTS direct_messages (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " time DATETIME DEFAULT CURRENT_TIMESTAMP,\n"
                + " sender TEXT NOT NULL,\n"
                + " receiver TEXT NOT NULL,\n"
                + " message TEXT NOT NULL\n"
                + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlUsers);
            stmt.execute(sqlMessages);
            stmt.execute(sqlDirectMessages);
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }

    public boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users(username, password) VALUES(?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean authenticateUser(String username, String password) {
        String sql = "SELECT username FROM users WHERE username = ? AND password = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Stores a chat message (public chat).
     */
    public void storeMessage(String sender, String text) {
        String sql = "INSERT INTO messages(sender, text) VALUES(?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sender);
            pstmt.setString(2, text);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error storing public message: " + e.getMessage());
        }
    }

    /**
     * Stores a direct message.
     */
    public void storeDirectMessage(String sender, String receiver, String text) {
        String sql = "INSERT INTO direct_messages(sender, receiver, message) VALUES(?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sender);
            pstmt.setString(2, receiver);
            pstmt.setString(3, text);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error storing direct message: " + e.getMessage());
        }
    }

    public List<Message> getChatHistory(int limit) {
        List<Message> history = new ArrayList<>();
        String sql = "SELECT sender, text FROM messages ORDER BY timestamp DESC LIMIT ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            List<Message> reversedHistory = new ArrayList<>();
            while (rs.next()) {
                String sender = rs.getString("sender");
                String text = rs.getString("text");
                reversedHistory.add(Message.history(sender, text));
            }

            for (int i = reversedHistory.size() - 1; i >= 0; i--) {
                history.add(reversedHistory.get(i));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving chat history: " + e.getMessage());
        }
        return history;
    }

    public List<Message> getDirectMessageHistory(String userA, String userB, int limit) {
        List<Message> history = new ArrayList<>();
        // Truy vấn DM giữa A->B hoặc B->A
        String sql = "SELECT time, sender, receiver, message FROM direct_messages " +
                "WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) " +
                "ORDER BY time DESC LIMIT ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userA);
            pstmt.setString(2, userB);
            pstmt.setString(3, userB);
            pstmt.setString(4, userA);
            pstmt.setInt(5, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                List<Message> reversedHistory = new LinkedList<>();
                while (rs.next()) {
                    String time = rs.getString("time");
                    String sender = rs.getString("sender");
                    String message = rs.getString("message");
                    reversedHistory.add(0, Message.directHistory(sender, message, time));
                }
                history.addAll(reversedHistory);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving direct message history: " + e.getMessage());
        }
        return history;
    }
}