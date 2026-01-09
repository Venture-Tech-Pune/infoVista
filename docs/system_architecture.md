# System Architecture - INFOVISTA

## 1. High-Level Overview

INFOVISTA is a real-time digital signage and notice management system consisting of three main components: a centralized Backend Server, an Android Mobile Application for administration, and a Python-based Display Application for digital screens.

```mermaid
graph TD
    subgraph "Admin / User"
        AndroidApp[📱 Android Mobile App]
        Admin[Admin/Manager]
        User[Student/Staff]
    end

    subgraph "Server Infrastructure"
        Backend[🚀 Node.js Backend API]
        DB[(🍃 MongoDB)]
        Socket[⚡ Socket.IO Server]
        Storage[📂 File Storage (Uploads)]
        FCM[🔥 Firebase Cloud Messaging]
    end

    subgraph "Digital Signage"
        DisplayApp[🖥️ Python Display App]
        Screen[Digital Screen]
    end

    %% Mobile App Interactions
    Admin -->|Login/Manage Notices| AndroidApp
    User -->|View Notices| AndroidApp
    AndroidApp -->|REST API (HTTP)| Backend
    AndroidApp -->|Upload Media| Backend
    Backend -->|Push Notifications| FCM -->|Notify| AndroidApp

    %% Backend Interactions
    Backend -->|Read/Write Data| DB
    Backend -->|Store Media| Storage
    Backend -->|Real-time Events| Socket

    %% Display App Interactions
    DisplayApp -->|REST API (Poll/Fetch)| Backend
    DisplayApp -->|WebSocket (Real-time)| Socket
    DisplayApp -->|Render UI| Screen
    Socket -->|Instant Updates| DisplayApp
```

## 2. Component Details

### A. Backend Server (Node.js + Express)
The core hub of the system handling logic, data persistence, and real-time communication.
- **API Layer**: RESTful endpoints for authentication, notice management, and device status.
- **Database**: MongoDB (via Mongoose) stores Users, Notices, and Device configurations.
- **Real-time Engine**: Socket.IO server broadcasts events (`notice:created`, `notice:deleted`) to connected displays efficiently.
- **Static File Server**: Serves uploaded images/videos for notices.
- **Session Management**: In-memory tracking to limit concurrent admin logins (Max 2).

### B. Android Mobile Application (Java)
The primary interface for users to interact with the system.
- **Admin/Manager Features**:
  - Secure Login with JWT.
  - CRUD Operations (Create, Read, Update, Delete) for notices.
  - Media Upload (Images) from device storage.
- **General User Features**:
  - View list of active notices.
  - Receive Push Notifications via Firebase (FCM).
- **Tech Stack**: Retrofit (Networking), Glide (Image Loading), Gson (Parsing).

### C. Display Application (Python + Pygame)
A lightweight client designed to run on Raspberry Pi or mini-PCs connected to large screens.
- **Rendering Engine**: Pygame for high-performance, full-screen graphics.
- **Hybrid Communication**: 
  - Uses **Socket.IO** for instant updates (new notices appear immediately).
  - Uses **Polling** as a backup for reliability.
- **Smart Layout**: Automatically adjusts grid/list layout based on active notice count.
- **Screensaver**: Displays Weather & Clock when no notices are active.
- **Caching**: Local caching of images to reduce bandwidth usage.

## 3. Data Flow & Processes

### Notice Creation & Display Flow
1. **Creation**: Admin creates a notice (text + image) in the **Android App**.
2. **Upload**: App sends `POST /api/notices` with multipart data to **Backend**.
3. **Storage**: Backend saves image to disk and metadata to **MongoDB**.
4. **Broadcast**: 
   - Backend emits `notice:created` event via **Socket.IO**.
   - Backend triggers **FCM** to send push notifications to mobile users.
5. **Display Update**: 
   - **Display App** receives Socket event.
   - Fetches new image/data immediately.
   - Re-renders the screen with the new notice without restarting.

### Security & Authentication
- **JWT (JSON Web Tokens)**: Used for stateless authentication across API requests.
- **Role-Based Access Control (RBAC)**: Middleware ensures only Admins/Managers can create/delete content.
- **Session Limiting**: Custom `SessionManager` enforces maximum simultaneous login limits for sensitive roles.

## 4. Technology Stack Summary

| Component | Technology | functionality |
|-----------|------------|---------------|
| **Backend** | Node.js, Express.js | API Server & Business Logic |
| **Database** | MongoDB | NoSQL Data Store |
| **Real-time** | Socket.IO | Instant bi-directional communication |
| **Mobile** | Java (Android SDK) | Admin & User Interface |
| **Display** | Python, Pygame | Digital Signage Player |
| **Network** | Retrofit, Requests | HTTP Client implementation |
| **Cloud** | Firebase (FCM) | Push Notifications |
