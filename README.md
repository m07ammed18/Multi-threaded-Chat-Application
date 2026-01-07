# Multi-threaded Chat Application

A simple yet powerful **multi-user chat application** built in **Java 23** with a modern Swing UI. This project demonstrates real-time communication between multiple clients through a multi-threaded server architecture, includes private messaging, file sharing, image previews, and a dark theme user interface.

---

## Overview

This application is a desktop-based chat system with a centralized server handling all clients. Each client connects using Java socket programming, and the server manages them concurrently using threads. The client application features a user-friendly graphical interface built with **Swing** and enhanced using **FlatLaf** for a dark mode look and feel.

The system supports **real-time messaging**, **private messages**, **file sharing**, **online user list**, and **system notifications** for user actions.

---

## Features

* **Multi-threaded Server** – Supports simultaneous connections from multiple clients.
* **User Authentication** – Username selection upon connecting.
* **Real-time Messaging** – Instant exchange of messages between users.
* **Private Messaging** – Direct messages to specific users via mentions.
* **File Sharing** – Send files to other users; images preview automatically.
* **Online Users List** – Displays currently connected users.
* **Dark Mode UI** – Sleek, modern user interface powered by FlatLaf.
* **Notifications** – System messages on user join/leave and connection status.

---

## Tech Stack

* **Java 23** – Core language for both server and client.
* **Swing** – Graphical user interface framework.
* **FlatLaf** – Modern dark theme for Swing applications.
* **Maven** – Dependency and build management.

---

## Getting Started

### Prerequisites

Before running the project locally:

1. Install **Java Development Kit (JDK) 23** or newer.
2. Install **Maven** for building and running the application.

---

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/m07ammed18/Multi-threaded-Chat-Application.git
   cd Multi-threaded-Chat-Application
   ```

2. **Build with Maven**

   ```bash
   mvn clean install
   ```

---

## Running the Project

### Start the Server

Launch the server first:

```bash
mvn exec:java -Dexec.mainClass="com.chatapp.server.ChatServer"
```

The server starts on **port 5000** by default.

### Start the Client

In a separate terminal, start the client:

```bash
mvn exec:java -Dexec.mainClass="com.chatapp.client.ClientMain"
```

Enter your username when prompted to connect to the server.

### Connect Multiple Clients

Repeat the client command in additional terminals to simulate multiple users joining the chat.

---

## Usage Guide

### Messaging

* Simply type a message and press **Enter** to broadcast it to all connected users.
* To send a **private message**, type:

  ```
  @username: your message
  ```

---

## File Sharing

* Click the **file icon** in the client UI to select and send a file.
* Images (e.g., `.jpg`, `.png`) display inline in the chat.
* Other file types prompt a download action for recipients.

---

## Configuration

* **Default host:** `localhost`
* **Default port:** `5000`
  These values can be modified in the client configuration (`ChatFrame` constructor) as needed.

---

## Project Structure

```
Multi-threaded-Chat-Application/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com.chatapp.server/
│   │   │   ├── com.chatapp.client/
│   │   │   ├── com.chatapp.ui/
│   │   │   └── resources/icons
├── pom.xml
└── README.md
```

---
# Acknowledgements

This project demonstrates practical application of socket programming, multi-threading, and desktop UI development using Java. It is suitable as a portfolio project or as an educational reference for real-time networked applications. 

