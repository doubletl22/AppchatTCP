package com.chat.server;

import com.chat.common.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            // Quan trọng: Khởi tạo ObjectOutputStream TRƯỚC ObjectInputStream
            // để tránh bị deadlock.
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            // Vòng lặp đọc đối tượng Message từ client
            while (true) {
                try {
                    Message clientMessage = (Message) in.readObject();
                    System.out.println("Nhận từ [" + clientMessage.sender() + "]: " + clientMessage.content());
                    Server.broadcastMessage(clientMessage);
                } catch (ClassNotFoundException e) {
                    System.err.println("Nhận được đối tượng không xác định.");
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            // Lỗi này thường xảy ra khi client đóng kết nối
            System.out.println("Client " + clientSocket.getInetAddress() + " đã ngắt kết nối.");
        } finally {
            closeConnections();
            Server.removeClient(this);
        }
    }

    /**
     * Gửi một đối tượng Message đến client này.
     * @param message Tin nhắn cần gửi.
     */
    public void sendMessage(Message message) {
        try {
            if (out != null) {
                out.writeObject(message);
                out.flush(); // Đảm bảo dữ liệu được gửi đi ngay lập tức
            }
        } catch (IOException e) {
            System.err.println("Không thể gửi tin nhắn đến client: " + e.getMessage());
            closeConnections();
        }
    }

    private void closeConnections() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
