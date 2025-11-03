package com.chat;

import java.sql.*;
import java.util.ArrayList;
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
        // Luu y: Trong ung dung that, MAT KHAU phai duoc HASH truoc khi luu.
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

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlUsers);
            stmt.execute(sqlMessages);
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }

    /**
     * Registers a new user.
     * @return true on success, false if username already exists or on error.
     */
    public boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users(username, password) VALUES(?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            // Error code 19 is typically unique constraint violation in SQLite (username exists)
            // if (e.getErrorCode() == 19) { // Removed for portability, but keep in mind
            //     return false;
            // }
            // Assuming any SQL error means registration failed (e.g., username exists)
            return false;
        }
    }

    /**
     * Authenticates a user.
     * @return true on success, false on failure.
     */
    public boolean authenticateUser(String username, String password) {
        String sql = "SELECT username FROM users WHERE username = ? AND password = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // true if a matching record is found
        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Stores a chat message.
     */
    public void storeMessage(String sender, String text) {
        String sql = "INSERT INTO messages(sender, text) VALUES(?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sender);
            pstmt.setString(2, text);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error storing message: " + e.getMessage());
        }
    }

    /**
     * Retrieves the last N messages.
     * @return A list of Message objects with type="history".
     */
    public List<Message> getChatHistory(int limit) {
        List<Message> history = new ArrayList<>();
        // Truy van N tin nhan moi nhat (ORDER BY timestamp DESC), sau do dao nguoc de hien thi theo thu tu thoi gian.
        String sql = "SELECT sender, text FROM messages ORDER BY timestamp DESC LIMIT ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            // Store in temporary list
            List<Message> reversedHistory = new ArrayList<>();
            while (rs.next()) {
                String sender = rs.getString("sender");
                String text = rs.getString("text");
                // Use Message.history to represent a historical message from the DB
                reversedHistory.add(Message.history(sender, text));
            }

            // Reverse the list to display oldest first (chronological order)
            for (int i = reversedHistory.size() - 1; i >= 0; i--) {
                history.add(reversedHistory.get(i));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving chat history: " + e.getMessage());
        }
        return history;
    }
}