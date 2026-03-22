# 🎬 CinéApp — JavaFX Cinema Management System

A full-featured JavaFX desktop application for cinema management with role-based access.

---

## 📋 Features

### Admin
- **Films** : Add, edit, delete (protected: no delete if future seances exist), filter by genre
- **Salles** : Add, edit, delete (protected), manage individual seats per hall
- **Séances** : Schedule screenings with conflict detection, edit/delete restrictions
- **Clients** : View all clients, edit their info, consult reservation history

### User (Client)
- **Browse movies** — Filter by genre, view descriptions
- **Reserve seats** — Visual seat map (green=available, red=taken, blue=selected, orange=VIP)
- **My reservations** — View history, download PDF ticket, cancel (if >2h before screening)
- **My profile** — Edit personal info, change password

---

## 🛠️ Setup

### 1. Prerequisites
- Java 17 or 21 JDK
- Maven 3.8+
- XAMPP (MySQL / MariaDB running)
- IntelliJ IDEA or any Java IDE

### 2. Database Setup

1. Start **XAMPP** → Start **Apache** and **MySQL**
2. Open **phpMyAdmin** → `http://localhost/phpmyadmin`
3. Import the base schema (provided by your professor):
   - Go to **Import** tab → choose the original `.sql` file → Execute
4. Then run the migration for user authentication:
   - Open the **SQL** tab in phpMyAdmin
   - Copy-paste the contents of `src/main/resources/migration_users.sql`
   - Click **Go**

### 3. Build & Run

```bash
# Clone / unzip the project
cd cinema_javafx

# Build
mvn clean install

# Run
mvn javafx:run
```

Or in IntelliJ: open the project, let Maven sync, then run `MainApp.java`.

---

## 🔐 Default Accounts

| Username | Password  | Role  |
|----------|-----------|-------|
| `admin`  | `admin123`| Admin |
| `marie`  | `marie123`| User  |

---

## 📦 Project Structure

```
cinema_javafx/
├── pom.xml
└── src/main/
    ├── java/com/cinema/
    │   ├── MainApp.java              ← Entry point
    │   ├── controller/
    │   │   ├── LoginController.java
    │   │   ├── RegisterController.java
    │   │   ├── SidebarController.java
    │   │   ├── FilmsController.java      (Admin)
    │   │   ├── SallesController.java     (Admin)
    │   │   ├── SeancesController.java    (Admin)
    │   │   ├── ClientsController.java    (Admin)
    │   │   ├── BrowseMoviesController.java (User)
    │   │   ├── MyReservationsController.java (User)
    │   │   └── ProfileController.java    (User)
    │   ├── dao/
    │   │   ├── UserDAO.java
    │   │   ├── FilmDAO.java
    │   │   ├── SalleDAO.java
    │   │   ├── SeanceDAO.java
    │   │   ├── ClientDAO.java
    │   │   └── ReservationDAO.java
    │   ├── model/
    │   │   ├── User.java
    │   │   ├── Film.java
    │   │   ├── Salle.java
    │   │   ├── Siege.java
    │   │   ├── Seance.java
    │   │   ├── Client.java
    │   │   └── Reservation.java
    │   ├── util/
    │   │   ├── DatabaseConnection.java
    │   │   ├── SessionManager.java
    │   │   ├── AlertUtil.java
    │   │   └── TicketPrinter.java
    │   └── view/
    │       └── ViewManager.java
    └── resources/com/cinema/
        ├── css/style.css
        └── migration_users.sql
```

---

## ⚙️ Database Config

Edit `DatabaseConnection.java` if needed:
```java
private static final String URL      = "jdbc:mysql://localhost:3306/cinema_db...";
private static final String USER     = "root";
private static final String PASSWORD = "";   // XAMPP default
```

---

## 🎫 PDF Tickets

Tickets are saved to your **home directory** (e.g. `C:\Users\you\ticket_CIN-2026-0001.pdf`).
iText 5 is included via Maven — no extra setup needed.

---

## 🏗️ Architecture

- **Pure JavaFX (Java-only)** — No FXML, all UI built programmatically
- **DAO pattern** — Clean separation between DB and UI
- **SessionManager** — Singleton holding logged-in user state
- **ViewManager** — Central navigation hub
- **Role-based routing** — Admin sees management panels; users see booking interface

---
