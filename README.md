# INFOVISTA: IoT-Based Smart Digital Board

**Final Year Project - SCOE&M 2025-2026**

A cloud-based smart digital notice board system that integrates IoT, cloud computing, and AI technologies for real-time multimedia announcements and environmental monitoring.

## Team Members

- **Kandekar Tanmay Ajit** (29)
- **Mavale Vaishnav Sopan** (38)
- **Shelke Rohan Satabhau** (45)

**Under Guidance of:** Prof. Suryavanshi J.S. (Computer Department)

---

## 📋 Project Overview

INFOVISTA revolutionizes traditional notice boards by providing:
- ✅ Real-time remote notice management via mobile and web apps
- ✅ Multimedia support (text, images, videos)
- ✅ AI-powered live weather forecasting and environmental monitoring
- ✅ Cloud-based synchronization across multiple display devices
- ✅ Role-based access control (Admin, Manager, Viewer)
- ✅ Scheduled and prioritized announcements
- ✅ Offline mode with cached content

---

## 🏗️ System Architecture

```
┌─────────────────┐
│  Android App    │ ← Admins/Managers create notices
└────────┬────────┘
         │
         ↓ (HTTPS/WebSocket)
┌─────────────────────────────┐
│   Backend Server (Node.js)  │
│   - User Authentication     │
│   - Notice Management       │
│   - Weather API Integration │
│   - Real-time Updates       │
└────────┬────────────────────┘
         │
         ↓ (HTTP/REST)
┌─────────────────────────────┐
│  Display Devices (Python)   │
│  - Raspberry Pi/Linux       │
│  - Notice Rendering         │
│  - Weather Widget           │
└─────────────────────────────┘
```

---

## 📁 Project Structure

```
infovista/
├── android/                  # Android Mobile Application
│   ├── app/
│   │   ├── src/main/java/com/example/infovista/
│   │   │   ├── models/      # Data models
│   │   │   ├── network/     # API client
│   │   │   ├── utils/       # Utilities
│   │   │   └── activities/  # App screens
│   │   └── res/             # Resources (layouts, drawables)
│   └── build.gradle
│
├── backend/                  # Node.js Backend Server
│   ├── config/              # Database configuration
│   ├── controllers/         # Business logic
│   ├── models/              # MongoDB schemas
│   ├── routes/              # API routes
│   ├── middleware/          # Auth & upload middleware
│   ├── uploads/             # Media files
│   └── server.js            # Main server file
│
└── display-app/             # Python Display Application
    ├── main.py              # Main application
    ├── api_client.py        # Backend API client
    ├── display_manager.py   # Display rendering
    ├── config.json          # Configuration
    └── requirements.txt     # Python dependencies
```

---

## 🚀 Quick Start Guide

### Prerequisites

1. **Backend:**
   - Node.js (v14+)
   - MongoDB (v4.4+)
   - OpenWeatherMap API Key

2. **Android App:**
   - Android Studio (latest version)
   - Android SDK 24+
   - Java JDK 11+

3. **Display App:**
   - Python 3.7+
   - Linux/Raspberry Pi OS
   - Display/monitor

### Installation Steps

#### 1. Backend Setup

```bash
cd backend

# Install dependencies
npm install

# Create .env file
cp .env.example .env

# Edit .env with your configuration
# Add MongoDB URI and Weather API key

# Start server
npm run dev
```

Server will run on `http://localhost:3000`

#### 2. Android App Setup

1. Open `android/` in Android Studio
2. Wait for Gradle sync to complete
3. Update `Constants.java`:
   ```java
   public static final String BASE_URL = "http://YOUR_IP:3000/";
   ```
4. Build and run on emulator or device

#### 3. Display App Setup

```bash
cd display-app

# Install dependencies
pip3 install -r requirements.txt

# Edit config.json with your server URL
nano config.json

# Run the display app
python3 main.py
```

---

## 🔧 Technology Stack

### Backend
- **Runtime:** Node.js + Express.js
- **Database:** MongoDB + Mongoose
- **Authentication:** JWT (JSON Web Tokens)
- **File Upload:** Multer
- **Real-time:** Socket.IO
- **Weather API:** OpenWeatherMap

### Android App
- **Language:** Java
- **UI:** Material Design Components
- **Network:** Retrofit + OkHttp
- **Image Loading:** Glide
- **Real-time:** Socket.IO Client
- **Architecture:** MVC Pattern

### Display App
- **Language:** Python 3
- **UI/Graphics:** Pygame
- **Image Processing:** Pillow (PIL)
- **HTTP Client:** Requests
- **Platform:** Linux/Raspberry Pi

---

## 📱 Features

### Mobile Application
- ✅ User registration and login
- ✅ Create, edit, delete notices
- ✅ Upload images and videos
- ✅ Set priority (Low, Medium, High, Urgent)
- ✅ Schedule notices with expiry
- ✅ Real-time synchronization
- ✅ User profile management

### Backend Server
- ✅ RESTful API architecture
- ✅ JWT-based authentication
- ✅ Role-based authorization
- ✅ File upload handling
- ✅ Weather data caching
- ✅ Socket.IO for real-time events
- ✅ MongoDB for data persistence

### Display Application
- ✅ Automatic notice rotation
- ✅ Live weather widget
- ✅ Multimedia rendering (images, videos)
- ✅ Priority-based display
- ✅ Offline mode with cached content
- ✅ Auto-reconnection on network failure
- ✅ Device status reporting

---

## 🌐 API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - User login
- `GET /api/auth/me` - Get current user
- `PUT /api/auth/update-profile` - Update profile
- `PUT /api/auth/change-password` - Change password

### Notices
- `GET /api/notices` - Get all notices (paginated)
- `GET /api/notices/active` - Get active notices
- `GET /api/notices/:id` - Get single notice
- `POST /api/notices` - Create notice (with file upload)
- `PUT /api/notices/:id` - Update notice
- `DELETE /api/notices/:id` - Delete notice

### Devices
- `GET /api/devices` - Get all devices
- `POST /api/devices` - Register device (Admin only)
- `PUT /api/devices/:deviceId/status` - Update device status

### Weather
- `GET /api/weather/current` - Get current weather
- `GET /api/weather/forecast` - Get forecast

---

## 🎯 User Roles

| Role | Permissions |
|------|-------------|
| **Admin** | Full access - manage users, devices, and notices |
| **Manager** | Create and manage notices |
| **Viewer** | Read-only access to notices |

---

## 📊 Database Schema

### User Collection
```json
{
  "name": "string",
  "email": "string (unique)",
  "password": "string (hashed)",
  "role": "admin | manager | viewer",
  "isActive": "boolean",
  "createdAt": "date"
}
```

### Notice Collection
```json
{
  "title": "string",
  "description": "string",
  "priority": "low | medium | high | urgent",
  "category": "general | academic | event | emergency | announcement",
  "mediaType": "text | image | video",
  "mediaUrl": "string",
  "scheduledAt": "date",
  "expiresAt": "date",
  "isActive": "boolean",
  "displayDuration": "number (seconds)",
  "targetDevices": ["deviceId"],
  "createdBy": "userId",
  "viewCount": "number"
}
```

---

## 🔐 Security Features

- ✅ Password hashing with bcrypt
- ✅ JWT token-based authentication
- ✅ Role-based access control (RBAC)
- ✅ HTTPS encryption (production)
- ✅ Input validation and sanitization
- ✅ CORS configuration
- ✅ File upload size limits
- ✅ SQL injection prevention (NoSQL database)

---

## 🧪 Testing

### Backend Testing
```bash
cd backend
npm test
```

### Manual Testing with Postman
1. Import Postman collection (if available)
2. Test authentication endpoints
3. Test notice CRUD operations
4. Test file uploads
5. Test real-time updates

### Android App Testing
1. Run on Android emulator
2. Test user registration and login
3. Create notices with media
4. Verify real-time synchronization

### Display App Testing
1. Run on Linux/Raspberry Pi
2. Verify notice fetching from server
3. Check weather widget display
4. Test offline mode

---

## 🐛 Troubleshooting

### Backend Issues
- **MongoDB connection error:** Ensure MongoDB is running
- **Port already in use:** Change PORT in .env
- **Weather API error:** Verify API key in .env

### Android App Issues
- **Network error:** Check BASE_URL in Constants.java
- **Build error:** Sync Gradle and rebuild
- **Image upload fails:** Check file size (max 10MB)

### Display App Issues
- **Connection refused:** Verify backend server URL in config.json
- **Display not showing:** Check if pygame is installed
- **Weather widget missing:** Enable in config.json

---

## 📝 Future Enhancements

- [ ] Multi-language support
- [ ] Voice announcements (Text-to-Speech)
- [ ] Advanced analytics dashboard
- [ ] Email/SMS notifications
- [ ] Mobile app for iOS
- [ ] Web admin panel
- [ ] Video streaming support
- [ ] Geofencing for location-based notices

---

## 📄 License

This project is developed as a Final Year Project for educational purposes at Samarth College of Engineering and Management, Belhe.

---

## 👥 Contributors

- **Development Team:** Kandekar Tanmay, Mavale Vaishnav, Shelke Rohan
- **Project Guide:** Prof. Suryavanshi J.S.
- **Institution:** SCOE&M, Belhe

---

## 📞 Contact

For queries related to this project, please contact:
- Email: [your-email@example.com]
- Institution: Samarth College of Engineering and Management

---

**© 2025-2026 SCOE&M Final Year Project**
