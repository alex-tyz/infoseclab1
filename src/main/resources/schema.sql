CREATE TABLE IF NOT EXISTS users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL
);
CREATE TABLE IF NOT EXISTS notes (
                                     id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     owner      VARCHAR(50)   NOT NULL,
    title      VARCHAR(100)  NOT NULL,
    content    VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner) REFERENCES users(username)
);