-- =============================================================
--  CINÉMA DATABASE - Script SQL Complet FINAL
--  Compatible XAMPP / MySQL / MariaDB
-- =============================================================

CREATE DATABASE IF NOT EXISTS cinema_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE cinema_db;

-- -------------------------------------------------------------
-- 1. TABLE : films
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS films (
    id_film        INT AUTO_INCREMENT PRIMARY KEY,
    titre          VARCHAR(200) NOT NULL,
    duree          INT NOT NULL,
    genre          VARCHAR(50)  NOT NULL,
    description    TEXT,
    date_sortie    DATE         NOT NULL,
    classification VARCHAR(20)  NOT NULL,
    affiche_path   VARCHAR(300) DEFAULT NULL,
    actif          TINYINT(1)   NOT NULL DEFAULT 1,
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- 2. TABLE : salles
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS salles (
    id_salle   INT AUTO_INCREMENT PRIMARY KEY,
    numero     VARCHAR(20)  NOT NULL UNIQUE,
    capacite   INT          NOT NULL,
    type_salle ENUM('2D','3D','IMAX','VIP','4DX') DEFAULT '2D',
    actif      TINYINT(1)   DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- 3. TABLE : sieges
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sieges (
    id_siege     INT AUTO_INCREMENT PRIMARY KEY,
    id_salle     INT         NOT NULL,
    rangee       VARCHAR(5)  NOT NULL,
    numero_siege INT         NOT NULL,
    type_siege   ENUM('STANDARD','VIP','PMR') DEFAULT 'STANDARD',
    UNIQUE KEY uq_siege (id_salle, rangee, numero_siege),
    FOREIGN KEY (id_salle) REFERENCES salles(id_salle) ON DELETE CASCADE
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- 4. TABLE : seances
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS seances (
    id_seance   INT AUTO_INCREMENT PRIMARY KEY,
    id_film     INT            NOT NULL,
    id_salle    INT            NOT NULL,
    date_heure  DATETIME       NOT NULL,
    prix_billet DECIMAL(8,2)   NOT NULL,
    langue      VARCHAR(30)    DEFAULT 'VF',
    statut      ENUM('PLANIFIEE','EN_COURS','TERMINEE','ANNULEE') DEFAULT 'PLANIFIEE',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_film)  REFERENCES films(id_film),
    FOREIGN KEY (id_salle) REFERENCES salles(id_salle)
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- 5. TABLE : clients
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS clients (
    id_client      INT AUTO_INCREMENT PRIMARY KEY,
    nom            VARCHAR(100) NOT NULL,
    prenom         VARCHAR(100) NOT NULL,
    email          VARCHAR(150) UNIQUE NOT NULL,
    telephone      VARCHAR(20),
    date_naissance DATE,
    actif          TINYINT(1)   DEFAULT 1,
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- 6. TABLE : users
--    id_client est NULL pour les admins (pas de profil client)
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(80)  NOT NULL UNIQUE,
    password_hash VARCHAR(64)  NOT NULL,
    role          ENUM('ADMIN','USER') NOT NULL DEFAULT 'USER',
    id_client     INT          NULL DEFAULT NULL,
    actif         TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_client) REFERENCES clients(id_client) ON DELETE CASCADE
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- 7. TABLE : reservations
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reservations (
    id_reservation   INT AUTO_INCREMENT PRIMARY KEY,
    id_client        INT            NOT NULL,
    id_seance        INT            NOT NULL,
    date_reservation DATETIME       DEFAULT CURRENT_TIMESTAMP,
    statut           ENUM('EN_ATTENTE','CONFIRMEE','ANNULEE') DEFAULT 'EN_ATTENTE',
    prix_total       DECIMAL(10,2)  DEFAULT 0.00,
    reference        VARCHAR(20)    UNIQUE NOT NULL,
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_client) REFERENCES clients(id_client),
    FOREIGN KEY (id_seance) REFERENCES seances(id_seance)
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- 8. TABLE : reservation_sieges
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reservation_sieges (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    id_reservation INT NOT NULL,
    id_siege       INT NOT NULL,
    UNIQUE KEY uq_res_siege (id_reservation, id_siege),
    FOREIGN KEY (id_reservation) REFERENCES reservations(id_reservation) ON DELETE CASCADE,
    FOREIGN KEY (id_siege)       REFERENCES sieges(id_siege)
) ENGINE=InnoDB;

-- -------------------------------------------------------------
-- 9. TABLE : etat_sieges_seance
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS etat_sieges_seance (
    id_seance INT,
    id_siege  INT,
    etat      ENUM('DISPONIBLE','RESERVE','OCCUPE') DEFAULT 'DISPONIBLE',
    PRIMARY KEY (id_seance, id_siege),
    FOREIGN KEY (id_seance) REFERENCES seances(id_seance) ON DELETE CASCADE,
    FOREIGN KEY (id_siege)  REFERENCES sieges(id_siege)   ON DELETE CASCADE
) ENGINE=InnoDB;

-- =============================================================
--  DATA — Salles
-- =============================================================

INSERT INTO salles (numero, capacite, type_salle) VALUES
('Salle 1',   120, '2D'),
('Salle 2',    80, '3D'),
('Salle 3',    60, 'IMAX'),
('Salle VIP',  30, 'VIP'),
('Salle 4DX',  50, '4DX');

-- =============================================================
--  DATA — Sieges
-- =============================================================

-- Salle 1 : 10 rangees x 12 sieges = 120 STANDARD
INSERT INTO sieges (id_salle, rangee, numero_siege, type_siege)
SELECT 1, CHAR(64+r), s, 'STANDARD'
FROM
  (SELECT 1 r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
   UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) AS r1,
  (SELECT 1 s UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
   UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
   UNION SELECT 11 UNION SELECT 12) AS c1;

-- Salle 2 : 8 rangees x 10 sieges = 80 STANDARD
INSERT INTO sieges (id_salle, rangee, numero_siege, type_siege)
SELECT 2, CHAR(64+r), s, 'STANDARD'
FROM
  (SELECT 1 r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
   UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) AS r2,
  (SELECT 1 s UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
   UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) AS c2;

-- Salle 3 IMAX : 6 rangees x 10 sieges = 60 STANDARD
INSERT INTO sieges (id_salle, rangee, numero_siege, type_siege)
SELECT 3, CHAR(64+r), s, 'STANDARD'
FROM
  (SELECT 1 r UNION SELECT 2 UNION SELECT 3
   UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) AS r3,
  (SELECT 1 s UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
   UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) AS c3;

-- Salle VIP : 3 rangees x 10 sieges = 30 VIP
INSERT INTO sieges (id_salle, rangee, numero_siege, type_siege)
SELECT 4, CHAR(64+r), s, 'VIP'
FROM
  (SELECT 1 r UNION SELECT 2 UNION SELECT 3) AS r4,
  (SELECT 1 s UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
   UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) AS c4;

-- Salle 4DX : 5 rangees x 10 sieges = 50 STANDARD
INSERT INTO sieges (id_salle, rangee, numero_siege, type_siege)
SELECT 5, CHAR(64+r), s, 'STANDARD'
FROM
  (SELECT 1 r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) AS r5,
  (SELECT 1 s UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
   UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) AS c5;

-- =============================================================
--  DATA — Films exemples
-- =============================================================

INSERT INTO films (titre, duree, genre, description, date_sortie, classification) VALUES
('Dune : Partie Deux',          166, 'Science-Fiction',
 'Paul Atreides unit les Fremen pour venger sa famille et affronter les Harkonnen.',
 '2024-03-01', 'AP12'),
('Oppenheimer',                 180, 'Drame',
 'Histoire de J. Robert Oppenheimer et de la creation de la bombe atomique.',
 '2023-07-19', 'AP12'),
('Kung Fu Panda 4',              94, 'Animation',
 'Po devient le Gardien spirituel et doit trouver un successeur.',
 '2024-03-08', 'Tout public'),
('La Zone d interet',           105, 'Drame historique',
 'Le commandant d Auschwitz et sa femme construisent une vie idyllique a cote du camp.',
 '2024-01-31', 'AP16'),
('Ghostbusters: Frozen Empire',  115, 'Aventure',
 'Les Ghostbusters affrontent une force ancienne qui menace de plonger le monde dans le gel.',
 '2024-03-22', 'AP10'),
('Le Comte de Monte-Cristo',    178, 'Aventure',
 'Edmond Dantes, injustement emprisonne, s evade et planifie sa vengeance.',
 '2024-06-28', 'AP12');

-- =============================================================
--  DATA — Seances exemples
-- =============================================================

INSERT INTO seances (id_film, id_salle, date_heure, prix_billet, langue, statut) VALUES
(1, 3, DATE_ADD(NOW(), INTERVAL 1 DAY),  18.00, 'VF',     'PLANIFIEE'),
(1, 3, DATE_ADD(NOW(), INTERVAL 2 DAY),  18.00, 'VO',     'PLANIFIEE'),
(2, 1, DATE_ADD(NOW(), INTERVAL 2 DAY),  12.00, 'VF',     'PLANIFIEE'),
(2, 1, DATE_ADD(NOW(), INTERVAL 3 DAY),  12.00, 'VOSTFR', 'PLANIFIEE'),
(3, 2, DATE_ADD(NOW(), INTERVAL 1 DAY),  10.00, 'VF',     'PLANIFIEE'),
(3, 2, DATE_ADD(NOW(), INTERVAL 4 DAY),  10.00, 'VF',     'PLANIFIEE'),
(5, 5, DATE_ADD(NOW(), INTERVAL 5 DAY),  15.00, 'VF',     'PLANIFIEE'),
(6, 4, DATE_ADD(NOW(), INTERVAL 6 DAY),  22.00, 'VF',     'PLANIFIEE');

-- =============================================================
--  Initialiser les etats des sieges pour toutes les seances
-- =============================================================

INSERT IGNORE INTO etat_sieges_seance (id_seance, id_siege, etat)
SELECT se.id_seance, sg.id_siege, 'DISPONIBLE'
FROM seances se
JOIN sieges sg ON sg.id_salle = se.id_salle;

-- =============================================================
--  DATA — Clients exemples
-- =============================================================

INSERT INTO clients (nom, prenom, email, telephone, actif) VALUES
('Dupont',  'Marie',  'marie.dupont@email.com',  '+216 22 333 444', 1),
('Ben Ali', 'Ahmed',  'ahmed.benali@email.com',  '+216 55 111 222', 1),
('Martin',  'Sophie', 'sophie.martin@email.com', '+216 98 765 432', 1);

-- =============================================================
--  DATA — Comptes utilisateurs
--  id_client = NULL pour admin (pas de profil client)
-- =============================================================

-- admin / admin123
INSERT INTO users (username, password_hash, role, id_client) VALUES
('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a', 'ADMIN', NULL);

-- marie / marie123
INSERT INTO users (username, password_hash, role, id_client)
SELECT 'marie', 'a9ef4a143e76e82a6e3de8afadab72b543e61413c15ad0e42a5efe81dee5db8f', 'USER', id_client
FROM clients WHERE email = 'marie.dupont@email.com';

-- ahmed / ahmed123
INSERT INTO users (username, password_hash, role, id_client)
SELECT 'ahmed', 'b3cc16b977c4fb38bbaaab52f6e32d07e82c1c8ef5e17e6e8d89da4f27cf49aa', 'USER', id_client
FROM clients WHERE email = 'ahmed.benali@email.com';

-- =============================================================
--  PROCEDURES STOCKEES
-- =============================================================

DELIMITER $$

CREATE PROCEDURE IF NOT EXISTS init_etat_sieges(IN p_id_seance INT)
BEGIN
    DECLARE v_id_salle INT;
    SELECT id_salle INTO v_id_salle FROM seances WHERE id_seance = p_id_seance;
    INSERT IGNORE INTO etat_sieges_seance (id_seance, id_siege, etat)
    SELECT p_id_seance, id_siege, 'DISPONIBLE'
    FROM sieges WHERE id_salle = v_id_salle;
END$$

CREATE PROCEDURE IF NOT EXISTS gen_reference(OUT p_ref VARCHAR(20))
BEGIN
    DECLARE v_year  VARCHAR(4);
    DECLARE v_count INT;
    SET v_year = YEAR(NOW());
    SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(reference,'-',-1) AS UNSIGNED)),0)+1
    INTO v_count
    FROM reservations
    WHERE reference LIKE CONCAT('CIN-',v_year,'-%');
    SET p_ref = CONCAT('CIN-',v_year,'-',LPAD(v_count,4,'0'));
END$$

DELIMITER ;

-- =============================================================
SELECT 'cinema_db creee avec succes !' AS message;
SELECT 'Comptes : admin/admin123  |  marie/marie123  |  ahmed/ahmed123' AS comptes;
-- =============================================================
