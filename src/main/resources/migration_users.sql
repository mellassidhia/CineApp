-- ============================================================
--  MIGRATION — Add users table to cinema_db
--  Run this ONCE after importing the base cinema_db schema
-- ============================================================

USE cinema_db;

-- Users table (linked to clients for USER role, id_client=0 for ADMIN)
CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(80)  NOT NULL UNIQUE,
    password_hash VARCHAR(64)  NOT NULL,          -- SHA-256 hex
    role          ENUM('ADMIN','USER') NOT NULL DEFAULT 'USER',
    id_client     INT NOT NULL DEFAULT 0,          -- 0 = admin account
    actif         TINYINT(1) NOT NULL DEFAULT 1,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_client) REFERENCES clients(id_client) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
--  DEFAULT ADMIN ACCOUNT
--  username : admin
--  password : admin123
--  SHA-256 of "admin123" = 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a
-- ============================================================
INSERT IGNORE INTO users (username, password_hash, role, id_client) VALUES
('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a', 'ADMIN', 0);

-- ============================================================
--  SAMPLE USER ACCOUNT (optional — for testing)
--  First create a sample client, then the user linked to it
-- ============================================================
INSERT IGNORE INTO clients (nom, prenom, email, telephone, actif) VALUES
('Dupont', 'Marie', 'marie.dupont@email.com', '+216 22 333 444', 1);

-- Get the id of the client we just inserted and create the user
SET @cid = (SELECT id_client FROM clients WHERE email = 'marie.dupont@email.com');

--  username : marie
--  password : marie123
--  SHA-256 of "marie123" = a9ef4a143e76e82a6e3de8afadab72b543e61413c15ad0e42a5efe81dee5db8f
INSERT IGNORE INTO users (username, password_hash, role, id_client) VALUES
('marie', 'a9ef4a143e76e82a6e3de8afadab72b543e61413c15ad0e42a5efe81dee5db8f', 'USER', @cid);

SELECT 'Migration complete ✅ — admin/admin123 and marie/marie123 created.' AS message;
