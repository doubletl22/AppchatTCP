# Ứng dụng Chat Nâng cao (Spring Boot + JavaFX)

Đây là một ứng dụng chat thời gian thực, đầy đủ tính năng, có khả năng mở rộng, được xây dựng với máy chủ Spring Boot WebSocket và client bằng JavaFX.

## ✨ Tính năng

  - **Nhắn tin thời gian thực**: Giao tiếp dựa trên WebSocket.
  - **Kiến trúc có khả năng mở rộng**: Sử dụng Redis Pub/Sub để đồng bộ hóa tin nhắn trên nhiều phiên bản máy chủ.
  - **Xác thực bằng JWT**: Bảo mật các endpoint của WebSocket và REST.
  - **Lưu trữ tin nhắn bền vững**: Lịch sử trò chuyện được lưu vào cơ sở dữ liệu PostgreSQL.
  - **REST API**: Dùng để xác thực và lấy lịch sử tin nhắn có phân trang.
  - **Client bằng JavaFX**: Giao diện người dùng hiện đại với FXML, giao diện tối (dark theme), và bong bóng chat.
  - **Được container hóa**: Hoàn toàn được container hóa với Docker và Docker Compose để dễ dàng triển khai.
  - **Sẵn sàng cho CI/CD**: Bao gồm một quy trình làm việc (workflow) của GitHub Actions để build, test, và đẩy (push) các Docker image.

## 🛠️ Yêu cầu cần có

  - JDK 17 hoặc mới hơn
  - Apache Maven 3.8+
  - Docker & Docker Compose
  - Một IDE như IntelliJ IDEA hoặc VS Code

## 🚀 Chạy trên máy cục bộ với Docker

Đây là cách được khuyến nghị để chạy toàn bộ hệ thống.

1.  **Sao chép (clone) repository:**

    ```bash
    git clone <your-repo-url>
    cd chat-advanced
    ```

2.  **Tạo file `.env`:**
    Sao chép file mẫu và tùy chỉnh nếu cần (các giá trị mặc định hoạt động ngay lập tức với Docker Compose).

    ```bash
    cp .env.example .env
    ```

3.  **Khởi động các dịch vụ:**
    Lệnh này sẽ build Docker image của máy chủ và khởi động các container `chat-server`, `postgres`, và `redis`.

    ```bash
    docker compose up --build -d
    ```

    Để xem log: `docker compose logs -f chat-server`

4.  **Chạy Client JavaFX:**

      - Mở thư mục `client-javafx` trong IDE của bạn.
      - Chạy lớp `MainApp.java`.
      - Để chạy một client thứ hai, chỉ cần chạy lại `MainApp.java` một lần nữa.

## 🧪 Cách kiểm tra

1.  Chạy hai phiên bản của client JavaFX.
2.  Trong cửa sổ đầu tiên, đăng nhập với tên người dùng `alice`.
3.  Trong cửa sổ thứ hai, đăng nhập với tên người dùng `bob`.
4.  Gửi tin nhắn từ `alice`. Chúng sẽ xuất hiện ngay lập tức trong cửa sổ của `bob`, và ngược lại.
5.  Thử lệnh `/clear` trong một client để xóa giao diện hiển thị cục bộ của nó.

## 🔐 Kích hoạt TLS với NGINX

File `docker-compose.yml` có bao gồm một dịch vụ `nginx` đã được bình luận (commented-out). Để kích hoạt nó:

1.  Tạo một file `./nginx/nginx.conf` với cấu hình để làm reverse proxy đến `chat-server:8080`.
2.  Bỏ bình luận (uncomment) dịch vụ `nginx` trong `docker-compose.yml`.
3.  Sử dụng **Certbot** trên máy chủ của bạn để lấy chứng chỉ SSL:
    ```bash
    sudo certbot certonly --standalone -d your.domain.com
    ```
4.  Cập nhật các đường dẫn volume trong dịch vụ `nginx` để trỏ đến các chứng chỉ Let's Encrypt đang hoạt động của bạn (`/etc/letsencrypt/live/your.domain.com`).
5.  File `nginx.conf` của bạn nên lắng nghe trên cổng 443 (SSL) và proxy các kết nối WebSocket (`/chat`) với các header phù hợp (`Upgrade`, `Connection`).
6.  Khởi động lại docker compose: `docker compose up -d --build nginx`.
7.  Cập nhật `SERVER_URL` trong file `AuthService.java` của client JavaFX để sử dụng `wss://your.domain.com`.

-----

### DANH SÁCH KIỂM TRA & Kiểm tra nhanh

Đây là cách để xác minh nhanh rằng máy chủ đang chạy đúng cách sau khi chạy `docker compose up`.

1.  **Kiểm tra Health Endpoint:**

    ```bash
    curl http://localhost:8080/api/health
    # Kết quả mong đợi: {"status":"UP"}
    ```

2.  **Kiểm tra API Đăng nhập:**

    ```bash
    curl -X POST -H "Content-Type: application/json" \
         -d '{"username":"alice", "password":"password123"}' \
         http://localhost:8080/api/auth/login
    # Kết quả mong đợi: {"token":"ey..."}
    ```

3.  **Kiểm tra API Lịch sử tin nhắn (với token hợp lệ):**
    Đầu tiên, lấy một token từ bước trên.

    ```bash
    TOKEN="your_jwt_token_here"
    curl -H "Authorization: Bearer ${TOKEN}" \
         "http://localhost:8080/api/messages?room=global&page=0&size=20"
    # Kết quả mong đợi: một mảng JSON chứa các tin nhắn, hoặc một mảng rỗng nếu chưa có.
    ```

4.  **Chạy hai Client JavaFX:**

      - Làm theo bước 4 trong phần "Chạy trên máy cục bộ".
      - Đăng nhập với tư cách `alice` và `bob`.
      - Xác minh rằng tin nhắn được gửi từ một client sẽ xuất hiện trên client còn lại. Điều này xác nhận toàn bộ vòng lặp: `Client -> Server -> Redis -> Server -> Client`.