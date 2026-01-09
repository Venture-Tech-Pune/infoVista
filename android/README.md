# INFOVISTA Android App Setup

## How to Run the Android Application

### Method 1: Using Android Studio

1. **Open the Project:**
   ```
   - Launch Android Studio
   - File → Open
   - Navigate to: d:\Projects 2025\Final Year projects\infovista\android
   - Click OK
   ```

2. **Wait for Gradle Sync:**
   - Android Studio will automatically sync Gradle dependencies
   - This may take a few minutes on first run

3. **Configure Backend URL:**
   - Open: `app/src/main/java/com/example/infovista/utils/Constants.java`
   - Update BASE_URL:
     ```java
     // For Android Emulator:
     public static final String BASE_URL = "http://10.0.2.2:3000/";
     
     // For Real Device (replace with your computer's IP):
     public static final String BASE_URL = "http://192.168.x.x:3000/";
     ```
   - To find your computer's IP:
     - Windows: Open CMD and run `ipconfig`
     - Look for IPv4 Address under your active network adapter

4. **Run the App:**
   - Connect an Android device via USB (with USB debugging enabled)
   - OR use an Android emulator (API 24 or higher recommended)
   - Click the green "Run" button or press Shift+F10
   - Select your device/emulator
   - Wait for the app to build and install

### Method 2: Build APK

```bash
cd android
gradlew assembleDebug
```

The APK will be generated at:
`android/app/build/outputs/apk/debug/app-debug.apk`

## Current Implementation Status

### ✅ Completed
- [x] Project structure and dependencies
- [x] Network layer (Retrofit + API client)
- [x] Data models (User, Notice, ApiResponse)
- [x] SharedPreferences manager
- [x] Constants and utilities

### 🔄 In Progress
- [ ] Login Activity
- [ ] Register Activity
- [ ] Dashboard Activity
- [ ] Create Notice Activity
- [ ] Notice Adapter (RecyclerView)

### 📝 To-Do
- [ ] Image/Video picker integration
- [ ] Real-time updates with Socket.IO
- [ ] Push notifications (FCM)
- [ ] Profile management
- [ ] UI polish and theming

## Project Structure

```
android/app/src/main/
├── java/com/example/infovista/
│   ├── models/
│   │   ├── User.java
│   │   ├── Notice.java
│   │   ├── ApiResponse.java
│   │   └── AuthResponse.java
│   ├── network/
│   │   ├── ApiClient.java
│   │   └── ApiService.java
│   ├── utils/
│   │   ├── Constants.java
│   │   └── SharedPrefManager.java
│   └── activities/ (to be created)
│       ├── LoginActivity.java
│       ├── DashboardActivity.java
│       └── CreateNoticeActivity.java
└── res/
    ├── layout/
    ├── drawable/
    └── values/
```

## Dependencies Added

- **Retrofit 2.9.0** - REST API client
- **Gson** - JSON serialization
- **OkHttp Logging Interceptor** - Network debugging
- **Glide 4.16.0** - Image loading
- **Socket.IO Client 2.1.0** - Real-time updates
- **Material Design Components** - Modern UI
- **ConstraintLayout** - Flexible layouts
- **CardView** - Card-based UI
- **RecyclerView** - List display

## Next Steps for Development

1. **Create Login Activity:**
   - Design login layout
   - Implement login API call
   - Save user session

2. **Create Dashboard:**
   - RecyclerView for notice list
   - FloatingActionButton to add notices
   - Pull-to-refresh functionality

3. **Create Notice Form:**
   - Input fields for title, description
   - Priority and category spinners
   - Image/video picker
   - Submit to backend

4. **Test End-to-End:**
   - Register new user
   - Login
   - Create notice
   - View on display app

## Troubleshooting

### Gradle Sync Failed
```bash
# Invalidate caches and restart
File → Invalidate Caches → Invalidate and Restart
```

### Network Connection Issues
- Ensure backend server is running on port 3000
- Check firewall settings
- For emulator: use 10.0.2.2 (not localhost)
- For device: ensure both are on same WiFi network

### Build Errors
```bash
# Clean and rebuild
Build → Clean Project
Build → Rebuild Project
```

## Useful Commands

```bash
# Check connected devices
adb devices

# Install APK manually
adb install app-debug.apk

# View logcat
adb logcat | grep InfoVista
```
