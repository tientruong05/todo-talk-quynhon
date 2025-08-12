-- Tạo database
CREATE DATABASE todo_task;
USE todo_task;

-- Bảng Users: Lưu thông tin người dùng
CREATE TABLE Users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    avatar_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB;

-- Bảng Chats: Quản lý các cuộc chat (1-1 hoặc nhóm)
CREATE TABLE Chats (
    chat_id INT AUTO_INCREMENT PRIMARY KEY,
    chat_name VARCHAR(100),
    is_group BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chat_id (chat_id)
) ENGINE=InnoDB;

-- Bảng Chat_Participants: Liên kết user với chat
CREATE TABLE Chat_Participants (
    participant_id INT AUTO_INCREMENT PRIMARY KEY,
    chat_id INT,
    user_id INT,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chat_id) REFERENCES Chats(chat_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    UNIQUE INDEX idx_chat_user (chat_id, user_id)
) ENGINE=InnoDB;

-- Bảng Messages: Lưu tin nhắn trong chat
CREATE TABLE Messages (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    chat_id INT,
    sender_id INT,
    content TEXT NOT NULL,
    is_todo_trigger BOOLEAN DEFAULT FALSE,
    edited_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chat_id) REFERENCES Chats(chat_id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    INDEX idx_chat_id (chat_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB;

-- Bảng Message_Reads: Lưu read receipts cho messages
CREATE TABLE Message_Reads (
    read_id INT AUTO_INCREMENT PRIMARY KEY,
    message_id INT,
    user_id INT,
    read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES Messages(message_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    UNIQUE INDEX idx_message_user (message_id, user_id)
) ENGINE=InnoDB;

-- Bảng Notifications: Lưu notifications (ví dụ: new message)
CREATE TABLE Notifications (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    type ENUM('new_message', 'new_task') NOT NULL,
    related_id INT NOT NULL, -- message_id hoặc task_id tùy type
    content VARCHAR(255),
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_type (type)
) ENGINE=InnoDB;

-- Bảng Tasks: Lưu task todo được tạo từ tin nhắn @Todo, gộp proof
CREATE TABLE Tasks (
    task_id INT AUTO_INCREMENT PRIMARY KEY,
    message_id INT,
    user_id INT,
    chat_id INT,
    description TEXT NOT NULL,
    status ENUM('pending', 'completed') DEFAULT 'pending',
    due_date DATETIME,
    proof_path VARCHAR(255), -- Đường dẫn file ảnh bằng chứng từ backend
    proof_uploaded_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES Messages(message_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (chat_id) REFERENCES Chats(chat_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_chat_id (chat_id)
) ENGINE=InnoDB;