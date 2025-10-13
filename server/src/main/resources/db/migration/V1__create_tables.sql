CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL,
  password VARCHAR(200) NOT NULL -- For POC, can be plain text. In prod, use bcrypt hash.
);

CREATE TABLE messages (
  id BIGSERIAL PRIMARY KEY,
  room VARCHAR(100) NOT NULL,
  from_user VARCHAR(50) NOT NULL,
  to_user VARCHAR(50), -- Can be NULL for room messages
  body TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX idx_messages_room_created_at ON messages(room, created_at DESC);
CREATE INDEX idx_users_username ON users(username);