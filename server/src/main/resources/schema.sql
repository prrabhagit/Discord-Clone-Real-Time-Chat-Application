-- ChatApp MySQL schema
-- Run as a user with CREATE privileges, e.g.:
--   mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS chatapp
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE chatapp;

-- Application user (adjust host/password as needed, or use your own user).
-- CREATE USER IF NOT EXISTS 'chatapp_user'@'localhost' IDENTIFIED BY 'chatapp_password';
-- GRANT ALL PRIVILEGES ON chatapp.* TO 'chatapp_user'@'localhost';
-- FLUSH PRIVILEGES;

-- ---------------------------------------------------------------------------
-- Users & presence
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS users (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(20)  NOT NULL UNIQUE,
    password_hash   VARCHAR(100) NOT NULL,
    avatar_color    VARCHAR(7)   NOT NULL DEFAULT '#5865F2',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS presence (
    user_id     INT PRIMARY KEY,
    status      ENUM('ONLINE', 'IDLE', 'DO_NOT_DISTURB', 'OFFLINE') NOT NULL DEFAULT 'OFFLINE',
    last_seen   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Friends
-- ---------------------------------------------------------------------------

-- Symmetric friendship: a row is inserted in both directions when a request
-- is accepted, so "friends of user X" is a single simple SELECT.
CREATE TABLE IF NOT EXISTS friends (
    user_id     INT NOT NULL,
    friend_id   INT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, friend_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS friend_requests (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    sender_id   INT NOT NULL,
    receiver_id INT NOT NULL,
    status      ENUM('PENDING', 'ACCEPTED', 'DECLINED') NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_receiver_status (receiver_id, status)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Servers (guilds) & channels
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS servers (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(50) NOT NULL,
    icon_text    VARCHAR(2)  NOT NULL DEFAULT '?',
    icon_color   VARCHAR(7)  NOT NULL DEFAULT '#5865F2',
    owner_id     INT NOT NULL,
    invite_code  VARCHAR(10) NOT NULL UNIQUE,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS server_members (
    server_id   INT NOT NULL,
    user_id     INT NOT NULL,
    joined_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (server_id, user_id),
    FOREIGN KEY (server_id) REFERENCES servers(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS channels (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    server_id   INT NOT NULL,
    name        VARCHAR(30) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (server_id) REFERENCES servers(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Messages (shared table for direct messages and channel messages)
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS messages (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id   INT NOT NULL,
    receiver_id INT NULL,   -- set for direct messages
    channel_id  INT NULL,   -- set for channel messages
    content     VARCHAR(2000) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (channel_id) REFERENCES channels(id) ON DELETE CASCADE,
    INDEX idx_dm_pair (sender_id, receiver_id, created_at),
    INDEX idx_channel (channel_id, created_at),
    CONSTRAINT chk_message_target CHECK (
        (receiver_id IS NOT NULL AND channel_id IS NULL) OR
        (receiver_id IS NULL AND channel_id IS NOT NULL)
    )
) ENGINE=InnoDB;
