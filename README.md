
# Java Swing TCP Chat (GUI)

- **GUI**: Swing (hiện đại hóa bằng layout hợp lý, có thể thêm FlatLaf nếu muốn).
- **Giao thức**: JSON line (mỗi thông điệp 1 dòng `\n`), dùng Gson.
- **Server**: đa luồng, broadcast, kick client, log GUI.
- **Client**: kết nối/ngắt, soạn/gửi, hiển thị chat & system.

## Yêu cầu
- JDK 25
- Java 17+
- Maven 3+
- Chạy trên Intellij

## Hướng dẫn chạy 
- copy mã dán vô terninal
- chạy nhiều client thì dùng bấm play ở dòng dưới client cho nhanh
## Build
```bash
mvn -q -e -DskipTests package
```
Tạo file: `target/AppchatTCP-1.0.0.jar` (fat-jar, kèm deps).

## Chạy
Server GUI:
```bash
java -cp target/AppchatTCP-1.0.0.jar com.chat.ui.server.ServerView
```
Client GUI:
```bash
java -cp target/AppchatTCP-1.0.0.jar com.chat.AppLauncher   
```

## Thư mục
```
src/main/java/
└── com/chat/
    ├── core/                # LAYER: Service/Core Logic
    │   ├── ChatClientCore.java  # Logic mạng Client: Connect, recvLoop, send
    │   ├── ChatServerCore.java  # Logic mạng Server: acceptLoop, broadcast, auth
    │   ├── ClientStatusListener.java # Interface: Core -> Controller (Client)
    │   └── ServerLogListener.java    # Interface: Core -> Controller (Server)
    │
    ├── model/               # LAYER: Model / Data Structure
    │   └── Message.java         # Định nghĩa Protocol (JSON line) và Factory Methods
    │   └── ClientViewModel.java # Quản lý trạng thái UI tập trung (ViewModel)
    │
    ├── service/             # LAYER: Data Access / Persistence
    │   └── DatabaseManager.java # Thao tác với SQLite (Users, Messages, DM)
    │
    ├── ui/
    │   ├── client/          # MODULE: Client UI (MVP)
    │   │   ├── ClientView.java          # JFrame chính, lắp ráp các Panel (View)
    │   │   ├── ClientController.java    # Xử lý sự kiện, điều phối Core (Presenter)
    │   │   ├── action/              # Các lớp Action cho nút bấm (VD: ConnectAction)
    │   │   ├── panel/               # Các Panel nhỏ (VD: ClientChatPanel, ConnectPanel)
    │   │   └── dialog/              # Các Dialog (VD: LoginDialog)
    │   │
    │   └── server/          # MODULE: Server UI (MVP)
    │       ├── ServerView.java          # JFrame chính, lắp ráp Panel (View)
    │       ├── ServerController.java    # Xử lý sự kiện, điều phối Core (Presenter)
    │       └── action/              # Các lớp Action cho nút bấm (VD: StartServerAction)
    │
    ├── util/                # MODULE: Utilities
    │   └── UiUtils.java             # Threading helper (SwingUtilities), Look&Feel
    │
    └── AppLauncher.java     # Điểm khởi động chính: Setup Theme, chọn chạy Server/Client
```
