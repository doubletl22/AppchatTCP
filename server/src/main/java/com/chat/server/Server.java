package com.chat.server;

import com.chat.common.Message;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final int PORT = 12345;
    // Danh sách an toàn luồng để lưu trữ tất cả các trình xử lý client
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    // Sử dụng một thread pool để quản lý các luồng hiệu quả hơn
    private static final ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        System.out.println("Server đang khởi động...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đang lắng nghe trên cổng " + PORT);

            while (true) {
                // Chấp nhận kết nối mới
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client mới đã kết nối: " + clientSocket.getInetAddress().getHostAddress());

                // Tạo một trình xử lý client mới và thực thi nó trên thread pool
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                pool.execute(clientHandler);
            }
        } catch (IOException e) {
            System.err.println("Lỗi server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            pool.shutdown();
        }
    }

    /**
     * Gửi một tin nhắn đến tất cả các client.
     * @param message Tin nhắn cần gửi.
     */
    public static void broadcastMessage(Message message) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    /**
     * Xóa một client khỏi danh sách (khi họ ngắt kết nối).
     * @param clientHandler Client cần xóa.
     */
    public static void removeClient(ClientHandler clientHandler) {
        synchronized (clients) {
            clients.remove(clientHandler);
            System.out.println("Một client đã ngắt kết nối. Số client còn lại: " + clients.size());
        }
    }
}
