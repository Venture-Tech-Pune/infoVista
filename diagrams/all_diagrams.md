# INFOVISTA Technical Diagrams (UML Style)

This document contains the Mermaid.js source code for all the technical diagrams required for your project report. 

> [!TIP]
> **How to export to PNG:**
> 1. Copy the code block for the diagram you want.
> 2. Go to [Mermaid Live Editor](https://mermaid.live/).
> 3. Paste the code into the left panel.
> 4. Use the "Actions" or "Download" button to save as **PNG**.

---

## 1. System Architecture Diagram (`architecture.png`)
```mermaid
graph TD
    subgraph "Cloud / Remote Server"
        Backend["Backend Server (Node.js/Express)"]
        DB[(MongoDB Database)]
        Storage["Media Storage (Images/Videos)"]
    end

    subgraph "Display Node (Raspberry Pi / Desktop)"
        App["main.py (Display App)"]
        DM["display_manager.py (Render Engine)"]
        Cache["Local Media Cache (Disk)"]
        Pygame["Pygame Graphics Canvas"]
    end

    Admin["Admin Dashboard (Web/Mobile)"] -- Post Notice --> Backend
    Backend --- DB
    Backend --- Storage

    App -- "REST API (GET /active)" --> Backend
    App -- "Socket.IO (Events)" --> Backend
    App -- "Download Media" --> Storage
    
    App --> DM
    DM --> Cache
    DM --> Pygame
    Pygame --> Monitor["Physical Monitor / LED Screen"]
```

---

## 2. System Flow Diagram (`system_flow.png`)
```mermaid
graph LR
    Start([Start]) --> Init[Initialize Pygame & Logging]
    Init --> LoadConfig[Load config.json]
    LoadConfig --> Connect[Socket.IO & API Connection]
    Connect --> Fetch[Initial Data Fetch]
    Fetch --> Loop{Main Loop}
    
    Loop --> Event[Listen for Socket Events]
    Loop --> Poll[Backup Periodic Polling]
    Loop --> Render[Calculate Grid & Render Canvas]
    
    Event -- New Notice --> Fetch
    Render --> Update[Update Display Buffer]
    Update --> Loop
    
    Loop -- Exit --> Stop([Graceful Shutdown])
```

---

## 3. ER Diagram (`er_diagram.png`)
```mermaid
erDiagram
    ADMIN ||--o{ NOTICE : "creates/manages"
    DEVICE ||--o{ NOTICE : "displays"
    NOTICE }|--|| CATEGORY : "belongs to"
    
    NOTICE {
        string _id PK
        string title
        string description
        string mediaUrl
        string mediaType
        string priority
        datetime createdAt
    }
    
    DEVICE {
        string deviceId PK
        string name
        string location
        string status
        datetime lastSeen
    }
    
    ADMIN {
        string userId PK
        string username
        string password
        string role
    }
```

---

## 4. Data Flow Diagrams (DFD)

### Level 0 (Context Diagram)
```mermaid
graph LR
    Admin((Admin/User)) -- "Create Notice" --> System[[INFOVISTA System]]
    System -- "Display Notice" --> Board((Display Board))
    System -- "Status/Weather" --> Board
    ExternalAPI((Weather API)) -- "Weather Data" --> System
```

### Level 1 (Process Breakdown)
```mermaid
graph TD
    Admin((Admin)) -- "Notice Data" --> P1[Manage Notices]
    P1 -- "Notice Storage" --> D1[(Database)]
    D1 -- "Active Notices" --> P2[Sync Engine]
    P2 -- "Raw Data" --> P3[Rendering Engine]
    P3 -- "Graphics" --> Board((Display Board))
    WeatherAPI((Weather API)) -- "Weather Info" --> P4[Weather Fetcher]
    P4 -- "Weather State" --> P3
```

### Level 2 (Rendering Subsystem)
```mermaid
graph TD
    Data[Notice JSON] --> G1[Calculate Grid Matrix]
    G1 --> G2[Calculate Box Coordinates]
    G2 --> G3[Load/Scale Media]
    G3 --> G4[Render Text wrapping]
    G4 --> G5[Blit to Surface]
    G5 --> Canvas[Pygame Buffer]
```

---

## 5. Use Case Diagram (`usecase.png`)
```mermaid
usecaseDiagram
    actor "Administrator" as Admin
    actor "Display Device" as Node
    actor "External Server" as Server

    package "INFOVISTA System" {
        usecase "Create notice with media" as UC1
        usecase "Delete / Update Notice" as UC2
        usecase "Monitor Device Heartbeat" as UC3
        usecase "Synchronize Real-time" as UC4
        usecase "Render Notice Grid" as UC5
        usecase "Display Weather & Clock" as UC6
    }

    Admin --> UC1
    Admin --> UC2
    Admin --> UC3
    
    UC4 <-- Node
    Node --> UC5
    Node --> UC6
    
    UC4 -- "WebSocket" -- Server
    UC1 -- "REST" -- Server
```

---

## 6. Class Diagram (`class_diagram.png`)
```mermaid
classDiagram
    class DisplayApp {
        +config: dict
        +notices: list
        +weather: dict
        +running: bool
        +sio: SocketIO
        +load_config()
        +setup_socketio()
        +fetch_notices()
        +run()
        +shutdown()
    }
    
    class DisplayManager {
        +screen: Surface
        +colors: dict
        +image_cache: dict
        +video_players: dict
        +draw_grid_layout()
        +draw_notice_box()
        +load_image()
        +load_video()
        +draw_empty_state()
    }
    
    class PiVideoPlayer {
        +path: string
        +width: int
        +height: int
        +get_frame()
        +close()
        -_reader()
    }
    
    class APIClient {
        +base_url: string
        +device_id: string
        +get_active_notices()
        +get_weather()
        +update_device_status()
    }

    DisplayApp *-- DisplayManager
    DisplayApp *-- APIClient
    DisplayManager *-- PiVideoPlayer
```

---

## 7. Sequence Diagram (`sequence_diagram.png`)
```mermaid
sequence_diagram
    participant A as Admin
    participant S as Backend Server
    participant C as Display Client (App)
    participant D as Display Manager (DM)

    A ->> S: Post New Notice (Title, Media)
    S ->> S: Save to DB
    S -->> C: Socket.IO Event (notice:created)
    C ->> S: GET /api/notices/active
    S -->> C: Return JSON Notice List
    C ->> C: Update local notice state
    C ->> D: draw_grid_layout(notices)
    D ->> S: Fetch Media URL
    S -->> D: Return Image/Video Data
    D ->> D: Render & Blit to Screen
```

---

## 8. Activity Diagram (`activity.png`)
```mermaid
stateDiagram-v2
    [*] --> Initializing
    Initializing --> FetchingData
    FetchingData --> Rendering
    
    state Rendering {
        [*] --> CheckEvents
        CheckEvents --> ProcessSockets
        ProcessSockets --> UpdateLayout
        UpdateLayout --> DisplayBuffer
        DisplayBuffer --> CheckEvents
    }

    CheckEvents --> ConnectionLost : Timeout
    ConnectionLost --> Retrying
    Retrying --> FetchingData
    
    Rendering --> [*] : Exit Signal
```

---

## 10. Collaboration Diagram (`collaboration.png`)
```mermaid
graph LR
    C[Display Client] <--> |1: Synchronize| S[Backend Server]
    C --> |2: Request Media| S
    S --> |3: Stream Data| C
    C --> |4: Update Layout| DM[Display Manager]
    DM --> |5: Cache Frames| Disk[Local Disk]
```

---

## 11. Statechart Diagram (`statechart.png`)
```mermaid
stateDiagram-v2
    [*] --> Off
    Off --> Booting: Power On
    Booting --> Idle: Load Config
    Idle --> Syncing: notice:created
    Syncing --> LoadingMedia: GET notices
    LoadingMedia --> Displaying: Success
    Displaying --> Idle: notice:deleted
    Displaying --> Displaying: notice:updated
    Displaying --> Error: Network Fail
    Error --> Syncing: Retry
```

---

## 12. Component Diagram (`component.png`)
```mermaid
graph TD
    subgraph "Display Application"
        [Socket.IO Client] -- Events --> [App Controller]
        [App Controller] -- Command --> [Display Engine]
        [Display Engine] -- Media --> [Video Player]
        [Display Engine] -- Media --> [Image Loader]
        [Display Engine] ..> [Media Cache]
        [App Controller] -- Requests --> [API Wrapper]
    end
    [API Wrapper] -- HTTP --> [External API]
```

---

## 13. Package Diagram (`package.png`)
```mermaid
graph TD
    subgraph "INFOVISTA Root"
        [backend]
        [android]
        [display-app]
        [docs]
        [diagrams]
    end
    
    subgraph "display-app package"
        [main.py] --> [display_manager.py]
        [main.py] --> [api_client.py]
        [display_manager.py] --> [cache]
    end
```
