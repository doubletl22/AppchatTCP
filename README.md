# Advanced Chat Application (Spring Boot + JavaFX)

This is a full-featured, scalable, real-time chat application built with a Spring Boot WebSocket server and a JavaFX client.

## ✨ Features

- **Real-time Messaging**: WebSocket-based communication.
- **Scalable Architecture**: Uses Redis Pub/Sub to synchronize messages across multiple server instances.
- **JWT Authentication**: Secure WebSocket and REST endpoints.
- **Message Persistence**: Chat history saved to a PostgreSQL database.
- **REST API**: For authentication and fetching message history with pagination.
- **JavaFX Client**: Modern UI with FXML, dark theme, and chat bubbles.
- **Containerized**: Fully containerized with Docker and Docker Compose for easy deployment.
- **CI/CD Ready**: Includes a GitHub Actions workflow for building, testing, and pushing Docker images.

## 🛠️ Prerequisites

- JDK 17 or newer
- Apache Maven 3.8+
- Docker & Docker Compose
- An IDE like IntelliJ IDEA or VS Code

## 🚀 Running Locally with Docker

This is the recommended way to run the entire stack.

1.  **Clone the repository:**
    ```bash
    git clone <your-repo-url>
    cd chat-advanced
    ```

2.  **Create `.env` file:**
    Copy the example file and customize if needed (the defaults work out-of-the-box with Docker Compose).
    ```bash
    cp .env.example .env
    ```

3.  **Start the services:**
    This command will build the server's Docker image and start the `chat-server`, `postgres`, and `redis` containers.
    ```bash
    docker compose up --build -d
    ```
    To see logs: `docker compose logs -f chat-server`

4.  **Run the JavaFX Client:**
    - Open the `client-javafx` directory in your IDE.
    - Run the `MainApp.java` class.
    - To run a second client, simply run `MainApp.java` again.

## 🧪 How to Test

1.  Run two instances of the JavaFX client.
2.  In the first window, log in with username `alice`.
3.  In the second window, log in with username `bob`.
4.  Send messages from `alice`. They should appear instantly in `bob`'s window, and vice-versa.
5.  Try the `/clear` command in one client to clear its local view.

## 🔐 Enabling TLS with NGINX

The `docker-compose.yml` includes a commented-out `nginx` service. To enable it:

1.  Create a `./nginx/nginx.conf` file with a configuration to reverse proxy to `chat-server:8080`.
2.  Uncomment the `nginx` service in `docker-compose.yml`.
3.  Use **Certbot** on your host machine to obtain SSL certificates:
    ```bash
    sudo certbot certonly --standalone -d your.domain.com
    ```
4.  Update the volume paths in the `nginx` service to point to your live Let's Encrypt certificates (`/etc/letsencrypt/live/your.domain.com`).
5.  Your `nginx.conf` should listen on port 443 (SSL) and proxy WebSocket connections (`/chat`) with appropriate headers (`Upgrade`, `Connection`).
6.  Restart docker compose: `docker compose up -d --build nginx`.
7.  Update the `SERVER_URL` in the JavaFX client's `AuthService.java` to use `wss://your.domain.com`.

---

### CHECKLIST & Quick Tests

Here's how to quickly verify the server is running correctly after `docker compose up`.

1.  **Check Health Endpoint:**
    ```bash
    curl http://localhost:8080/api/health
    # Expected output: {"status":"UP"}
    ```

2.  **Test Login API:**
    ```bash
    curl -X POST -H "Content-Type: application/json" \
         -d '{"username":"alice", "password":"password123"}' \
         http://localhost:8080/api/auth/login
    # Expected output: {"token":"ey..."}
    ```

3.  **Test Message History API (with a valid token):**
    First, get a token from the step above.
    ```bash
    TOKEN="your_jwt_token_here"
    curl -H "Authorization: Bearer ${TOKEN}" \
         "http://localhost:8080/api/messages?room=global&page=0&size=20"
    # Expected output: JSON array of messages, or an empty array if none.
    ```

4.  **Run Two JavaFX Clients:**
    - Follow step 4 in the "Running Locally" section.
    - Log in as `alice` and `bob`.
    - Verify that messages sent from one client appear on the other. This confirms the entire loop: `Client -> Server -> Redis -> Server -> Client`.