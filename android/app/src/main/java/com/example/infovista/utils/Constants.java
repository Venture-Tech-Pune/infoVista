package com.example.infovista.utils;

public class Constants {
    // API Configuration
    // Use 10.0.2.2 for Android Emulator, or your PC's IP address for physical device
    public static final String BASE_URL = "http://192.168.31.141:3000/"; // For real device
    
    // Endpoints
    public static final String API_AUTH = "api/auth/";
    public static final String API_NOTICES = "api/notices/";
    public static final String API_DEVICES = "api/devices/";
    public static final String API_WEATHER = "api/weather/";
    
    // SharedPreferences
    public static final String PREF_NAME = "InfoVistaPref";
    public static final String KEY_TOKEN = "auth_token";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_USER_NAME = "user_name";
    public static final String KEY_USER_EMAIL = "user_email";
    public static final String KEY_USER_ROLE = "user_role";
    public static final String KEY_IS_LOGGED_IN = "is_logged_in";
    
    // Request Codes
    public static final int REQUEST_IMAGE_PICK = 100;
    public static final int REQUEST_VIDEO_PICK = 101;
    public static final int REQUEST_PERMISSION = 200;
    
    // Notice Priorities
    public static final String PRIORITY_LOW = "low";
    public static final String PRIORITY_MEDIUM = "medium";
    public static final String PRIORITY_HIGH = "high";
    public static final String PRIORITY_URGENT = "urgent";
    
    // Notice Categories
    public static final String CATEGORY_GENERAL = "general";
    public static final String CATEGORY_ACADEMIC = "academic";
    public static final String CATEGORY_EVENT = "event";
    public static final String CATEGORY_EMERGENCY = "emergency";
    public static final String CATEGORY_ANNOUNCEMENT = "announcement";
    
    // User Roles
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_MANAGER = "manager";
    public static final String ROLE_VIEWER = "viewer";
}
