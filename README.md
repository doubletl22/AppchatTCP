
# Java Swing TCP Chat (GUI)

- **GUI**: Swing (hiện đại hóa bằng layout hợp lý, có thể thêm FlatLaf nếu muốn).
- **Giao thức**: JSON line (mỗi thông điệp 1 dòng `\n`), dùng Gson.
- **Server**: đa luồng, broadcast, kick client, log GUI.
- **Client**: kết nối/ngắt, soạn/gửi, hiển thị chat & system.

## Yêu cầu
- Java 17+
- Maven 3+

## Build
```bash
mvn -q -e -DskipTests package
```
Tạo file: `target/swing-tcp-chat-1.0.0.jar` (fat-jar, kèm deps).

## Chạy
Server GUI:
```bash
java -cp target/swing-tcp-chat-1.0.0.jar com.chat.server.ServerUI
```
Client GUI:
```bash
java -cp target/swing-tcp-chat-1.0.0.jar com.chat.client.ClientUI
```

## Gợi ý nâng cấp giao diện "hiện đại"
- Thêm **FlatLaf** (Light/Dark) để giao diện Swing hiện đại hơn:
  - Dependency:
    ```xml
    <dependency>
      <groupId>com.formdev</groupId>
      <artifactId>flatlaf</artifactId>
      <version>3.5.4</version>
    </dependency>
    ```
  - Ở `main`, thêm:
    ```java
    com.formdev.flatlaf.FlatLightLaf.setup();
    UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
    ```

## Thư mục
```
src/main/java/com/chat/Message.java
src/main/java/com/chat/server/ServerUI.java
src/main/java/com/chat/client/ClientUI.java
```
