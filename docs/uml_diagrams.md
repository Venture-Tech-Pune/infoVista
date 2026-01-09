# INFOVISTA - UML Diagrams

## 1. Class Diagram
This diagram represents the structure of the backend models and their relationships.

```mermaid
classDiagram
    class User {
        +String _id
        +String name
        +String email
        +String password
        +String role
        +String fcmToken
        +Date createdAt
        +getPublicProfile()
        +comparePassword()
    }

    class Notice {
        +String _id
        +String title
        +String description
        +String mediaUrl
        +String mediaType
        +String priority
        +String category
        +Date scheduledAt
        +Date expiresAt
        +Boolean isActive
        +User createdBy
        +List~Device~ targetDevices
    }

    class Device {
        +String _id
        +String deviceId
        +String name
        +String location
        +String status
        +Date lastSeen
        +String ipAddress
    }

    class Session {
        +String userId
        +String token
        +Date loginTime
        +String ip
    }

    User "1" --> "*" Notice : creates
    Notice "*" --> "*" Device : displays_on
    User "1" --> "*" Session : has_active
```

## 2. Sequence Diagram: Creating a Notice
This diagram illustrates the flow when an Admin creates a new notice.

```mermaid
sequenceDiagram
    participant Admin as 📱 Admin App
    participant API as 🚀 Backend API
    participant DB as 🍃 MongoDB
    participant Socket as ⚡ Socket.IO
    participant FCM as 🔥 Firebase
    participant Display as 🖥️ Display Screen

    Admin->>API: POST /api/notices (Token, Data, File)
    activate API
    
    API->>API: Validate Token & Role
    API->>API: Save File to Disk
    
    API->>DB: Save Notice Data
    activate DB
    DB-->>API: Notice Created
    deactivate DB

    par Real-time Updates
        API->>Socket: Emit 'notice:created'
        Socket-->>Display: Event: 'notice:created'
        Display->>API: GET /api/notices/active
        API-->>Display: JSON Data + Image URL
        Display->>Display: Download & Render Image
    and Push Notifications
        API->>FCM: Send Notification Payload
        FCM-->>Admin: Notification Sent
    end

    API-->>Admin: 201 Created Success
    deactivate API
```

## 3. Use Case Diagram
High-level overview of user interactions.

```mermaid
graph LR
    subgraph "INFOVISTA System"
        Login((Login))
        CreateNotice((Create Notice))
        EditNotice((Edit Notice))
        DeleteNotice((Delete Notice))
        ViewNotices((View Notices))
        UploadImage((Upload Image))
        ViewDisplay((View Display))
        Screensaver((Screensaver))
    end
    
    Admin[👤 Admin]
    User[👤 User]
    DisplayUnit[🖥️ Display Unit]

    Admin --> Login
    Admin --> CreateNotice
    Admin --> EditNotice
    Admin --> DeleteNotice
    Admin --> ViewNotices
    CreateNotice -.-> UploadImage

    User --> Login
    User --> ViewNotices

    DisplayUnit --> ViewDisplay
    DisplayUnit --> Screensaver
```

## 4. Activity Diagram: Notice Lifecycle
Flow of a notice from creation to display.

```mermaid
graph TD
    Start((Start)) --> Login[Admin Logs In]
    Login --> Dashboard[View Dashboard]
    Dashboard --> ClickCreate[Click 'Create Notice']
    ClickCreate --> FillForm[Fill Title, Desc, Priority]
    FillForm --> AddImage{Add Image?}
    AddImage -- Yes --> Upload[Select & Upload Image]
    AddImage -- No --> Submit
    Upload --> Submit[Submit Form]
    
    Submit --> Backend[Backend Processing]
    Backend -->|Valid| SaveDB[Save to DB]
    Backend -->|Invalid| Error[Show Error]
    
    SaveDB --> Broadcast[Broadcast Event]
    Broadcast --> DisplayUpdate[Display App Updates UI]
    DisplayUpdate --> End((End))
```
