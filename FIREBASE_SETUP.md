# Firebase Cloud Messaging Setup Guide

## Overview

Push notifications have been integrated using Firebase Cloud Messaging (FCM). This allows the app to receive real-time alerts when new notices are created.

## Setup Instructions

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Enter project name: **InfoVista** (or your preferred name)
4. Disable Google Analytics (optional)
5. Click "Create project"

### 2. Add Android App to Firebase

1. In Firebase Console, click "Add app" → select Android
2. Enter package name: `com.example.infovista`
3. Enter app nickname: **InfoVista Android**
4. Click "Register app"
5. Download `google-services.json`
6. Move the file to: `android/app/google-services.json`
7. Click "Next" and "Continue to console"

### 3. Enable Cloud Messaging

1. In Firebase Console, go to **Project Settings** (gear icon)
2. Navigate to **Cloud Messaging** tab
3. Under **Cloud Messaging API (Legacy)**, note your **Server Key**

### 4. Setup Backend (Node.js)

1. In Firebase Console, go to **Project Settings** → **Service Accounts**
2. Click "Generate new private key"
3. Download the JSON file
4. Rename it to `firebase-service-account.json`
5. Move it to: `backend/firebase-service-account.json`

6. Install Firebase Admin SDK:
```bash
cd backend
npm install firebase-admin
```

### 5. Test Push Notifications

#### From Android App:
1. Run the app and login
2. The app automatically registers for notifications
3. FCM token is sent to backend

#### From Backend:
1. Create a new notice via mobile app
2. All logged-in users should receive a push notification

## How It Works

### Android (Client Side)

1. **App Startup:**
   - Firebase SDK initializes automatically
   - FCM token is generated

2. **After Login:**
   - Token is sent to backend via `/api/notifications/register-token`
   - Stored in user's database record

3. **Receiving Notifications:**
   - `MyFirebaseMessagingService` handles incoming messages
   - Creates Android notification with title, body, and action

### Backend (Server Side)

1. **When Notice Created:**
   - Fetches all users with FCM tokens
   - Sends push notification to all tokens
   - Uses `firebase-admin` SDK

2. **Notification Format:**
   ```javascript
   {
     notification: {
       title: "New HIGH Notice",
       body: "Emergency Announcement"
     },
     data: {
       noticeId: "123...",
       priority: "high",
       category: "emergency"
     }
   }
   ```

## API Endpoints

### Register FCM Token
```http
POST /api/notifications/register-token
Authorization: Bearer <token>
Content-Type: application/json

{
  "fcmToken": "device_fcm_token_here"
}
```

### Unregister Token (Logout)
```http
DELETE /api/notifications/unregister-token
Authorization: Bearer <token>
```

## Testing with Firebase Console

You can manually send test notifications:

1. Go to Firebase Console → **Cloud Messaging**
2. Click "Send your first message"
3. Enter notification text
4. Click "Send test message"
5. Enter your FCM token (check app logs)
6. Click "Test"

## Notification Channels (Android 8.0+)

The app creates a notification channel:
- **Channel ID:** `infovista_notices`
- **Channel Name:** InfoVista Notices
- **Importance:** High
- **Sound:** Default notification sound

## Troubleshooting

### Notifications not received?

1. **Check google-services.json:**
   - Ensure it's in `android/app/` directory
   - Package name matches: `com.example.infovista`

2. **Check backend setup:**
   - `firebase-service-account.json` is present
   - Firebase Admin initialized successfully (check logs)

3. **Check permissions:**
   - Android 13+: Notification permission granted
   - Check: Settings → Apps → InfoVista → Notifications

4. **Check FCM token:**
   - Look for "FCM Token:" in Android logcat
   - Verify token is sent to backend

5. **Check backend logs:**
   - Look for "Notification sent successfully"
   - Check for Firebase errors

### Common Issues

**Error: "Default Firebase app does not exist"**
- Ensure `google-services.json` is properly placed
- Clean and rebuild project

**Error: "Firebase not initialized"**
- Check `firebase-service-account.json` in backend
- Verify file format and permissions

**Notifications work in foreground but not background:**
- This is expected - FCM handles background automatically
- Notifications should still appear in notification tray

## Security Considerations

1. **Never commit Firebase config files:**
   - `google-services.json`
   - `firebase-service-account.json`
   - Both are in `.gitignore`

2. **Restrict API Keys:**
   - In Firebase Console, restrict API keys to your app
   - Add SHA-1 fingerprint for production

3. **Token Security:**
   - FCM tokens are automatically refreshed
   - Old tokens are invalidated

## Production Deployment

1. Generate signed APK with release keystore
2. Add SHA-1 fingerprint to Firebase
3. Use production `google-services.json`
4. Secure backend with HTTPS
5. Monitor Firebase Console for delivery statistics

---

**Note:** Firebase has a free tier that includes unlimited FCM messages, perfect for this project!
