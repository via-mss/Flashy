# Flashy

Flashy is a desktop flashcard study application built with **Java** and **JavaFX**. It lets you create decks of flashcards, study them with a built-in spaced-repetition-style review mode, share decks with other users, and track your score over time. An admin role is included for user and content moderation. Developed as part of the **Java Basics II** course at **Vidzeme University of Applied Sciences**.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Setup & Running](#setup--running)
  - [Eclipse (recommended)](#eclipse-recommended)
  - [IntelliJ IDEA](#intellij-idea)
  - [Command Line](#command-line)
- [Default Credentials](#default-credentials)
- [Usage Guide](#usage-guide)
- [Data Storage](#data-storage)

---

## Features

| Feature | Details |
|---|---|
| **User accounts** | Register and log in with securely hashed passwords (PBKDF2 + random salt) |
| **Dashboard** | View your decks in a paginated card grid; double-click to jump straight into study mode |
| **Study mode** | Choose how many cards to review, flip each card, then rate it as *Known / Somewhat / Not Known* — no repeats within a session |
| **Scoring** | Earn points each session based on how well you knew the cards; cumulative score is persisted per user |
| **Deck editor** | Create or edit decks: add cards with front/back text and an optional image attachment; toggle a deck public or private |
| **Browse packs** | Discover public decks from other users and download a personal copy with one click |
| **Settings** | Update your display name, change your password, or switch between **dark** and **light** themes |
| **Admin panel** | Visible only to the `admin` account — ban/unban or delete users, and edit or delete any deck |

---

## Tech Stack

- **Language:** Java 11+ (JPMS module `finalwork`)
- **UI framework:** JavaFX 11+ (`javafx.controls`, `javafx.fxml`)
- **Database:** SQLite via the JDBC driver (`java.sql`), with automatic schema migration on first run
- **Password hashing:** PBKDF2WithHmacSHA256 (falls back to PBKDF2WithHmacSHA1)
- **Build tooling:** Eclipse e(fx)clipse / Ant (`build.fxbuild`)

---

## Prerequisites

| Requirement | Version |
|---|---|
| JDK | 11 or later |
| JavaFX SDK | Matching your JDK (11+). Many JDK distributions bundle it; otherwise download from [gluonhq.com/products/javafx](https://gluonhq.com/products/javafx/) |
| SQLite JDBC driver | [sqlite-jdbc](https://github.com/xerial/sqlite-jdbc) JAR (e.g. `sqlite-jdbc-3.x.x.jar`) |

> **No Maven or Gradle** — the project uses a plain source layout. Dependencies must be placed on the classpath/module path manually or via your IDE.

---

## Project Structure

```
Flashy/
├── build.fxbuild            # Eclipse e(fx)clipse Ant build descriptor
├── data/
│   ├── app.db               # SQLite database (auto-created on first run)
│   └── settings.properties  # Persisted user preferences (theme)
├── src/
│   ├── module-info.java     # JPMS module descriptor
│   └── application/
│       ├── Main.java                  # Application entry point
│       ├── Database.java              # SQLite data-access layer
│       ├── SessionData.java           # In-memory session state
│       ├── Deck.java / Flashcard.java # Domain model
│       ├── PasswordUtils.java         # PBKDF2 hashing utilities
│       ├── LoginController.java       # Login / registration screen
│       ├── DashboardController.java   # Main dashboard
│       ├── StudyModeController.java   # Flashcard review session
│       ├── DeckEditorController.java  # Create / edit decks
│       ├── BrowsePacksController.java # Discover public decks
│       ├── SettingsController.java    # User settings
│       ├── AdminController.java       # Admin moderation panel
│       ├── *.fxml                     # JavaFX layout files
│       ├── style.css                  # Base stylesheet
│       └── dark.css                   # Dark-theme overlay
└── bin/                     # Compiled class output (Eclipse default)
```

---

## Setup & Running

### Eclipse (recommended)

Eclipse with the **e(fx)clipse** plugin is the easiest way to open and run Flashy.

1. **Install Eclipse IDE for Java Developers** and add the [e(fx)clipse plugin](https://marketplace.eclipse.org/content/efxclipse) via *Help → Eclipse Marketplace*.
2. **Add the SQLite JDBC JAR** to the project:
   - Download `sqlite-jdbc-x.x.x.jar` from the [sqlite-jdbc releases page](https://github.com/xerial/sqlite-jdbc/releases).
   - Right-click the project → *Build Path → Configure Build Path → Libraries → Add External JARs* and select the downloaded JAR.
   - Also add it to the *Module Path* if Eclipse separates the two tabs.
3. **Configure JavaFX** (only if your JDK does not bundle JavaFX):
   - Download the JavaFX SDK that matches your JDK version.
   - Add all JARs from the `lib/` folder of the SDK to the build path (module path).
4. **Run the application:**
   - Open `src/application/Main.java`.
   - Right-click → *Run As → Java Application*.

### IntelliJ IDEA

1. Open the repository root as a new project (*File → Open*).
2. Go to *File → Project Structure → Libraries* and add:
   - The SQLite JDBC JAR.
   - All JARs from your JavaFX SDK `lib/` folder (if not already on the JDK).
3. Go to *Run → Edit Configurations*, create a new **Application** configuration:
   - **Main class:** `application.Main`
   - **VM options:**
     ```
     --module-path /path/to/javafx-sdk/lib
     --add-modules javafx.controls,javafx.fxml
     ```
   - Add the SQLite JDBC JAR to the classpath as well.
4. Run the configuration.

### Command Line

```bash
# From the repository root, compile the module
javac \
  --module-path /path/to/javafx-sdk/lib:/path/to/sqlite-jdbc.jar \
  --add-modules javafx.controls,javafx.fxml \
  -d bin \
  $(find src -name "*.java")

# Run
java \
  --module-path /path/to/javafx-sdk/lib:/path/to/sqlite-jdbc.jar:bin \
  --add-modules javafx.controls,javafx.fxml \
  -m finalwork/application.Main
```

> Replace `/path/to/javafx-sdk/lib` and `/path/to/sqlite-jdbc.jar` with the actual paths on your machine. On Windows use `;` instead of `:` as the path separator.

---

## Default Credentials

The database ships pre-seeded. If you are starting with a blank `data/app.db`, register a new user via the **Register** button on the login screen.

To access the **Admin Panel**, create (or use an existing) account with the username exactly **`admin`**. The admin button on the dashboard is shown only for this account.

---

## Usage Guide

1. **Log in or register** on the login screen.
2. **Dashboard** — your personal decks are shown here (3 per page). Use the arrow buttons to paginate.
   - Click a deck to select it, then press **Study** or **Edit**.
   - Double-click a deck to jump straight into a study session.
   - Press **Create** to make a new empty deck.
   - Press **Browse** to explore public decks shared by other users.
3. **Study mode** — enter how many cards to review (up to the deck size), then:
   - Click the card (or the card area) to flip it and reveal the answer.
   - Rate yourself: **Known**, **Somewhat**, or **Not Known**.
   - Once all cards have been reviewed the session summary and score are shown.
4. **Deck Editor** — rename the deck, toggle it **Public** (makes it visible in Browse Packs), add/delete cards, and optionally attach an image to each card.
5. **Browse Packs** — select a public deck and click **Download** to add a private copy to your dashboard.
6. **Settings** — change your display name, update your password, or switch between dark and light themes.
7. **Admin** (`admin` account only) — ban/unban or delete users; edit or delete any deck.

---

## Data Storage

- **SQLite database** (`data/app.db`) — stores users, decks, and cards. The schema is created and migrated automatically on first launch.
- **Settings** (`data/settings.properties`) — stores the last-used theme preference.
- Backup copies of the database (`app.db.bak_*`) are created automatically by the application.
