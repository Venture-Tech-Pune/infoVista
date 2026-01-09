# INFOVISTA Backend API

IoT-based Smart Digital Notice Board Backend Server

## Features

- ✅ User Authentication (JWT)
- ✅ Role-Based Access Control (Admin, Manager, Viewer)
- ✅ Notice Management (CRUD with scheduling)
- ✅ Device Management
- ✅ Real-time Updates (Socket.IO)
- ✅ Weather Integration (OpenWeatherMap)
- ✅ File Upload (Images & Videos)
- ✅ MongoDB Database

## Prerequisites

- Node.js (v14 or higher)
- MongoDB (v4.4 or higher)
- OpenWeatherMap API Key

## Installation

1. **Clone the repository**
   ```bash
   cd backend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Configure environment variables**
   ```bash
   cp .env.example .env
   ```
   
   Edit `.env` and add your configuration:
   ```
   MONGODB_URI=mongodb://localhost:27017/infovista
   JWT_SECRET=your_secure_secret_key
   WEATHER_API_KEY=your_openweathermap_api_key
   PORT=3000
   ```

4. **Create uploads directory**
   ```bash
   mkdir uploads
   ```

## Running the Server

### Development Mode
```bash
npm run dev
```

### Production Mode
```bash
npm start
```

The server will start on `http://localhost:3000`

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user
- `GET /api/auth/me` - Get current user
- `PUT /api/auth/update-profile` - Update profile
- `PUT /api/auth/change-password` - Change password

### Notices
- `GET /api/notices` - Get all notices (paginated)
- `GET /api/notices/active` - Get active notices (for display)
- `GET /api/notices/:id` - Get single notice
- `POST /api/notices` - Create notice (Admin/Manager)
- `PUT /api/notices/:id` - Update notice (Admin/Manager)
- `DELETE /api/notices/:id` - Delete notice (Admin/Manager)

### Devices
- `GET /api/devices` - Get all devices
- `GET /api/devices/:id` - Get single device
- `POST /api/devices` - Register device (Admin)
- `PUT /api/devices/:id` - Update device (Admin)
- `PUT /api/devices/:deviceId/status` - Update device status
- `DELETE /api/devices/:id` - Delete device (Admin)

### Weather
- `GET /api/weather/current` - Get current weather
- `GET /api/weather/forecast` - Get weather forecast

## Testing with Postman

### 1. Register Admin User
```http
POST http://localhost:3000/api/auth/register
Content-Type: application/json

{
  "name": "Admin User",
  "email": "admin@scoe.edu",
  "password": "admin123",
  "role": "admin"
}
```

### 2. Login
```http
POST http://localhost:3000/api/auth/login
Content-Type: application/json

{
  "email": "admin@scoe.edu",
  "password": "admin123"
}
```

### 3. Create Notice (use token from login)
```http
POST http://localhost:3000/api/notices
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: multipart/form-data

title: Emergency Announcement
description: Classes are cancelled tomorrow
priority: high
category: announcement
```

## Project Structure

```
backend/
├── config/
│   └── database.js          # MongoDB connection
├── controllers/
│   ├── authController.js    # Auth logic
│   ├── noticeController.js  # Notice management
│   ├── deviceController.js  # Device management
│   └── weatherController.js # Weather API
├── middleware/
│   ├── auth.js              # JWT verification
│   └── upload.js            # File upload
├── models/
│   ├── User.js              # User schema
│   ├── Notice.js            # Notice schema
│   └── Device.js            # Device schema
├── routes/
│   ├── auth.js              # Auth routes
│   ├── notices.js           # Notice routes
│   ├── devices.js           # Device routes
│   └── weather.js           # Weather routes
├── uploads/                 # Uploaded files
├── .env                     # Environment variables
├── .env.example             # Example env file
├── server.js                # Main server file
└── package.json             # Dependencies
```

## Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `PORT` | Server port | `3000` |
| `MONGODB_URI` | MongoDB connection string | `mongodb://localhost:27017/infovista` |
| `JWT_SECRET` | Secret for JWT tokens | `your_secret_key` |
| `WEATHER_API_KEY` | OpenWeatherMap API key | `abc123...` |
| `WEATHER_API_URL` | Weather API base URL | `https://api.openweathermap.org/data/2.5` |
| `MAX_FILE_SIZE` | Max upload size (bytes) | `10485760` (10MB) |
| `UPLOAD_DIR` | Upload directory | `uploads` |

## User Roles

- **Admin**: Full access (create users, manage devices, manage notices)
- **Manager**: Can create and manage notices
- **Viewer**: Read-only access

## Socket.IO Events

- `notice:created` - Emitted when notice is created
- `notice:updated` - Emitted when notice is updated
- `notice:deleted` - Emitted when notice is deleted
- `device:register` - Device registration event

## License

MIT
