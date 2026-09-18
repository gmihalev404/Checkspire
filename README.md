# ♞ Checkspire

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)
![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-purple)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Server--Side%20UI-green)

**Checkspire** is a full-stack web chess platform built with Spring Boot.

It supports real-time chess games, separate ratings for different time controls, player challenges, tournament management, spectating, game history, and responsive desktop/mobile gameplay.

The project was built as a complete multiplayer chess application rather than a simple chess board, with focus on backend domain logic, real-time communication, tournament systems, persistence, security, and a responsive frontend.

---

## 🌐 Live Demo

**Checkspire:**  
https://Checkspire-production.up.railway.app

---

## ✨ Features

### ♟ Real-Time Chess

- Real-time games using WebSocket and STOMP
- Interactive chess board
- Click-to-move and drag-and-drop movement
- White and Black board orientation
- Live chess clocks
- Increment-based time controls
- Move validation on the backend
- Last-move highlighting
- Pawn promotion with an interactive piece selector
- Draw offers
- Resignation
- Game abortion
- Automatic timeout handling
- PGN generation
- Move history
- Position review using previous game states
- Spectating of live and completed games

Only one game can be `IN_PROGRESS` for a player at a time.

---

### ⏱ Time Controls & Ratings

Checkspire maintains independent ratings for:

- Bullet
- Blitz
- Rapid
- Classical

Supported time controls include examples such as:

- `1 + 0`
- `2 + 1`
- `3 + 0`
- `3 + 2`
- `5 + 0`
- `10 + 0`
- `10 + 5`
- `15 + 10`
- `30 + 0`

Games can be either **Rated** or **Casual**.

Player ratings are updated according to the corresponding time-control category.

---

### ⚔ Challenges

Players can:

- Send challenges to other users
- Select a time control
- Choose a preferred color
- Create Rated or Casual games
- Accept incoming challenges
- Decline challenges
- Cancel outgoing challenges

When a challenge is accepted, Checkspire creates the game and redirects both players to the game board.

---

### 🏆 Tournaments

Checkspire includes a complete tournament system with support for:

- Swiss tournaments
- Round Robin tournaments
- Single Elimination tournaments
- Rated and Casual tournaments
- Maximum player limits
- Scheduled tournament start
- Automatic tournament start
- Manual tournament start
- Configurable break between rounds
- Scheduled round activation
- Manual early round start by the tournament creator
- Player joining and withdrawal
- Tournament forfeiting
- Automatic BYE handling
- Tournament standings
- Player seeding
- Round tracking
- Board numbers
- Match results
- Live tournament games
- Spectating tournament games

Tournament rounds have their own lifecycle:

```text
SCHEDULED
    ↓
IN_PROGRESS
    ↓
COMPLETED
```

The next round is prepared after the previous round completes and starts according to the configured round break.

---

### 👤 Users & Authentication

- User registration
- Spring Security authentication
- BCrypt password hashing
- Automatic authentication after registration
- Session-based authentication
- User account status
- User roles
- Separate chess ratings
- Responsive login and registration pages

Passwords are never stored as plain text.

---

### 📱 Responsive Interface

The frontend is built with:

- Thymeleaf
- Bootstrap
- Custom CSS
- Vanilla JavaScript

The interface is designed for both desktop and mobile devices.

It includes:

- Responsive navigation
- Compact game lists
- Mobile-friendly chess board
- Responsive tournament pages
- Scrollable challenge sections
- Player rating overview
- Dark Checkspire visual theme

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.1.1 |
| Web | Spring MVC |
| Security | Spring Security |
| Persistence | Spring Data JPA / Hibernate |
| Database | MySQL |
| Frontend | Thymeleaf, Bootstrap, CSS |
| Client Logic | JavaScript |
| Real-Time Communication | WebSocket + STOMP |
| Testing | JUnit, Mockito, Spring Boot Test |
| Test Database | H2 |
| Build Tool | Maven |
| Production Hosting | Railway |

---

## 🏗 Architecture

Checkspire follows a layered Spring architecture:

```text
Browser
   │
   │ HTTP / WebSocket
   ▼
Controllers
   │
   ▼
Application / Service Layer
   │
   ├──────────────► Chess Game Logic
   │
   ├──────────────► Tournament Logic
   │
   ├──────────────► Rating Logic
   │
   └──────────────► User / Challenge Logic
   │
   ▼
Repositories
   │
   ▼
MySQL
```

The project separates:

```text
Controller
    ↓
Application Service
    ↓
Domain Service
    ↓
Repository
    ↓
Database
```

This keeps web concerns, business logic, and persistence responsibilities separated.

---

## 📁 Project Structure

```text
src/
├── main/
│   ├── java/com/example/Checkspire/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── model/
│   │   │   ├── entity/
│   │   │   └── enums/
│   │   ├── repository/
│   │   └── service/
│   │
│   └── resources/
│       ├── static/
│       │   ├── css/
│       │   └── js/
│       │
│       ├── templates/
│       │   ├── challenge/
│       │   ├── fragments/
│       │   ├── game/
│       │   └── tournament/
│       │
│       ├── application.properties
│       └── application-prod.properties
│
└── test/
    ├── java/
    └── resources/
```

---

## 🚀 Running Locally

### Requirements

Make sure you have:

- Java 17+
- MySQL 8+
- Git

Maven installation is not required because the project includes the Maven Wrapper.

---

### 1. Clone the repository

```bash
git clone https://github.com/gmihalev404/Checkspire.git
cd Checkspire
```

---

### 2. Configure MySQL

Checkspire uses a local MySQL database named:

```text
Checkspire
```

The application can create the database automatically if it does not already exist.

Set your MySQL password as an environment variable.

#### Windows PowerShell

```powershell
$env:DB_PASSWORD="your_mysql_password"
```

#### Linux / macOS

```bash
export DB_PASSWORD="your_mysql_password"
```

The default local configuration expects:

```text
Host: localhost
Port: 3306
Username: root
Database: Checkspire
```

---

### 3. Start Checkspire

#### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

#### Linux / macOS

```bash
./mvnw spring-boot:run
```

Then open:

```text
http://localhost:8080
```

---

## 🧪 Running Tests

Checkspire contains unit and integration tests for the main application logic.

#### Windows

```powershell
.\mvnw.cmd clean test
```

#### Linux / macOS

```bash
./mvnw clean test
```

Tests use an H2 in-memory database and do not modify the development MySQL database.

---

## 🌍 Production

Checkspire is deployed on **Railway**.

Production consists of:

```text
Internet
    │
    │ HTTPS / WSS
    ▼
Checkspire
Spring Boot
    │
    │ Private Network
    ▼
Railway MySQL
```

The production Spring profile reads its configuration from environment variables:

```text
SPRING_PROFILES_ACTIVE=prod

DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
PORT
```

Sensitive database credentials are not stored in the repository.

---

## 🔄 Real-Time Game Flow

A typical game flow is:

```text
Player A sends challenge
          ↓
Player B accepts
          ↓
Game is created
          ↓
Both players open the board
          ↓
WebSocket connection established
          ↓
Player makes move
          ↓
STOMP message
          ↓
Spring backend validates move
          ↓
Game state saved
          ↓
Updated state broadcast
          ↓
Both clients update instantly
```

The server remains authoritative for game state and move validation.

---

## 🏆 Tournament Flow

```text
Tournament Created
        ↓
Registration
        ↓
Participants Join
        ↓
Tournament Starts
        ↓
Round Pairings Generated
        ↓
Games Played
        ↓
Round Completed
        ↓
Next Round Scheduled
        ↓
...
        ↓
Final Round Completed
        ↓
Tournament Finished
```

Depending on the tournament format, Checkspire generates the appropriate pairings and determines progression between rounds.

---

## 🔐 Security

Checkspire uses Spring Security and includes:

- BCrypt password hashing
- Authenticated game routes
- Authenticated tournament routes
- Authenticated challenge routes
- Server-side authorization checks
- Session-based authentication
- WebSocket authentication
- CSRF protection for web forms

Sensitive production configuration is supplied through environment variables.

---

## 🎯 Project Goals

Checkspire was built to practice and demonstrate:

- Spring Boot application architecture
- Complex relational domain modelling
- Spring Data JPA
- Transaction management
- Spring Security
- Real-time WebSocket communication
- Multiplayer state synchronization
- Tournament algorithms
- Server-side rendering with Thymeleaf
- Responsive web design
- Unit and integration testing
- Production deployment and configuration

---

## 📌 Possible Future Improvements

Potential future additions include:

- Player profiles
- Friend system UI
- Matchmaking queue
- Leaderboards
- Rating history graphs
- Opening statistics
- Game analysis
- Computer opponents
- Notifications
- Custom tournament invitations
- Email verification
- Password reset
- Flyway database migrations
- Persistent distributed sessions
- Additional production monitoring

---

## ♞ Checkspire

> **Your board. Your rating. Your forge.**
