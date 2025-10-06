package com.chat.client;

import com.chat.common.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.function.Consumer;

public class ClientService {

    private static final String HOST = "localhost";
    private static final int PORT = 12345;
    private ObjectOutputStream out;
    private final Consumer<Message> onMessageReceived;

    public ClientService(Consumer<Message> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
    }

    public void connect() {
        try {
            Socket socket = new Socket(HOST, PORT);
            System.out.println("Đã kết nối tới server.");

            out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // Luồng riêng để lắng nghe tin nhắn từ server
            new Thread(() -> {
                try {
                    while (true) {
                        Message serverMessage = (Message) in.readObject();
                        onMessageReceived.accept(serverMessage);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("Mất kết nối tới server.");
                    onMessageReceived.accept(new Message("HỆ THỐNG", "Mất kết nối tới server."));
                }
            }).start();

        } catch (UnknownHostException e) {
            onMessageReceived.accept(new Message("HỆ THỐNG", "Không tìm thấy server."));
        } catch (IOException e) {
            onMessageReceived.accept(new Message("HỆ THỐNG", "Lỗi kết nối: " + e.getMessage()));
        }
    }

    public void sendMessage(Message message) {
        try {
            if (out != null) {
                out.writeObject(message);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("Không thể gửi tin nhắn: " + e.getMessage());
        }
    }
}
