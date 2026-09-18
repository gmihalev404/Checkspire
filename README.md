# ♞ Checkspire

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)
![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-purple)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Server--Side%20UI-green)

**Checkspire** is a full-stack online chess platform built with Spring Boot, focused on competitive play and tournament management.

The platform supports real-time chess games, independent ratings for different time controls, player challenges, tournament management, spectating, game history, and responsive desktop/mobile gameplay.

Checkspire is designed as a complete multiplayer chess application rather than a simple chess board, with emphasis on backend domain logic, real-time communication, tournament systems, persistence, security, and a responsive frontend.

---

## 🌐 Live Demo

**Checkspire:**  
[Checkspire](https://checkspire-production.up.railway.app/)

---

## 🏆 Tournament Platform

Tournament play is one of the main focuses of Checkspire.

Supported tournament formats include:

- **Swiss**
- **Round Robin**
- **Single Elimination**

Tournaments support:

- Rated and Casual events
- Maximum player limits
- Scheduled tournament starts
- Automatic tournament starts
- Manual tournament starts
- Configurable breaks between rounds
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
- Spectating

Tournament rounds have their own lifecycle:

```text
SCHEDULED
    ↓
IN_PROGRESS
    ↓
COMPLETED
```

After a round is completed, Checkspire prepares the next round according to the tournament format and configured round break.

---

## ♟ Real-Time Chess

Checkspire includes a complete browser-based chess game system:

- Real-time games using WebSocket and STOMP
- Interactive chess board
- Click-to-move
- Drag-and-drop movement
- White and Black board orientation
- Live chess clocks
- Increment-based time controls
- Server-side move validation
- Last-move highlighting
- Pawn promotion
- Draw offers
- Resignation
- Game abortion
- Automatic timeout handling
- PGN generation
- Move history
- Previous-position review
- Spectating live games
- Spectating completed games

The server remains authoritative for game state and move validation.

A player can have only one game with status `IN_PROGRESS` at a time.

---

## ⏱ Time Controls & Ratings

Checkspire maintains independent ratings for:

- Bullet
- Blitz
- Rapid
- Classical

Supported time controls currently include:

```text
1 + 0
2 + 1
3 + 0
3 + 2
5 + 0
10 + 0
10 + 5
15 + 10
30 + 0
```

Games can be either **Rated** or **Casual**.

When a rated game finishes, the appropriate rating category is updated according to the game's time control.

---

## ⚔ Player Challenges

Registered players can:

- Send challenges to other users
- Select a time control
- Choose a preferred color
- Choose between Rated and Casual play
- Accept incoming challenges
- Decline challenges
- Cancel outgoing challenges

When a challenge is accepted, Checkspire creates the game and both players can enter the live game board.

---

## 🔎 Matchmaking Lobby

Checkspire includes a public `/play` lobby designed as the entry point for future matchmaking.

The lobby supports separate interfaces for:

- Guest players
- Authenticated players

The planned matchmaking system will allow guests to play Casual games without creating an account, while authenticated players will be able to use account-based matchmaking.

Guest matchmaking itself is currently under development.

---

## 👤 Users & Authentication

The account system includes:

- User registration
- Spring Security authentication
- BCrypt password hashing
- Automatic authentication after registration
- Session-based authentication
- User roles
- User account status
- Separate chess ratings

Passwords are never stored as plain text.

---

## 👁 Spectating & Game History

Users can follow live and completed games without participating in them.

Spectators can view:

- The chess board
- Player information
- Chess clocks
- Move history
- Previous board positions
- Game result
- Tournament context

Spectators cannot make moves or perform player-only actions.

---

## 📱 Responsive Interface

The frontend is built with:

- Thymeleaf
- Bootstrap
- Custom CSS
- Vanilla JavaScript

The interface uses a dark competitive chess design with graphite surfaces and amber accents.

It includes:

- Responsive navigation
- Responsive chess board
- Compact game lists
- Tournament pages
- Challenge management
- Player rating overview
- Public About page
- Public tournament discovery
- Matchmaking lobby
- Mobile-friendly layouts

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

The application separates web concerns, business logic, and persistence responsibilities.

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

---

## 📁 Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── com/example/chessforge/
│   │       ├── config/
│   │       ├── controller/
│   │       ├── model/
│   │       │   ├── entity/
│   │       │   └── enums/
│   │       ├── repository/
│   │       └── service/
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

You need:

- Java 17+
- MySQL 8+
- Git

A separate Maven installation is not required because the repository includes the Maven Wrapper.

### 1. Clone the repository

```bash
git clone https://github.com/gmihalev404/Checkspire.git
cd Checkspire
```

### 2. Configure MySQL

The local application uses a MySQL database named:

```text
checkspire
```

The database can be created automatically when the application starts.

Set the MySQL password through the `DB_PASSWORD` environment variable.

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
Database: checkspire
```

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

The project contains unit and integration tests for the main application and domain logic.

### Windows

```powershell
.\mvnw.cmd clean test
```

### Linux / macOS

```bash
./mvnw clean test
```

Tests use an H2 in-memory database and do not modify the local MySQL development database.

---

## 🌍 Production

Checkspire is deployed on **Railway**.

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

The production Spring profile reads configuration from environment variables:

```text
SPRING_PROFILES_ACTIVE=prod

DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
PORT
```

Sensitive production credentials are not stored in the repository.

---

## 🔄 Real-Time Game Flow

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

Pairing and progression behavior depends on whether the tournament uses Swiss, Round Robin, or Single Elimination format.

---

## 🔐 Security

Checkspire uses Spring Security and includes:

- BCrypt password hashing
- Protected authenticated functionality
- Server-side authorization checks
- Session-based authentication
- WebSocket authentication
- CSRF protection for web forms
- Environment-based production configuration

Public pages such as the homepage, About page, matchmaking lobby, and tournament discovery can be accessed without authentication.

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
- Chess game logic
- Tournament algorithms
- Server-side rendering with Thymeleaf
- Responsive web design
- Unit and integration testing
- Production deployment and configuration

---

## 📌 Planned Improvements

Future additions may include:

- Guest matchmaking
- Account-based matchmaking
- Player profiles
- Friend system UI
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

> **Play. Compete. Rise.**
