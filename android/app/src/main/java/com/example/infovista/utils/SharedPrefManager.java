package com.example.infovista.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {
    private static SharedPrefManager instance;
    private Context context;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private SharedPrefManager(Context context) {
        this.context = context.getApplicationContext();
        sharedPreferences = this.context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context);
        }
        return instance;
    }

    // Save user login data
    public void saveUser(String token, String userId, String name, String email, String role) {
        editor.putString(Constants.KEY_TOKEN, token);
        editor.putString(Constants.KEY_USER_ID, userId);
        editor.putString(Constants.KEY_USER_NAME, name);
        editor.putString(Constants.KEY_USER_EMAIL, email);
        editor.putString(Constants.KEY_USER_ROLE, role);
        editor.putBoolean(Constants.KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    // Check if user is logged in
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(Constants.KEY_IS_LOGGED_IN, false);
    }

    // Get auth token
    public String getToken() {
        return sharedPreferences.getString(Constants.KEY_TOKEN, null);
    }

    // Get user ID
    public String getUserId() {
        return sharedPreferences.getString(Constants.KEY_USER_ID, null);
    }

    // Get user name
    public String getUserName() {
        return sharedPreferences.getString(Constants.KEY_USER_NAME, "User");
    }

    // Get user email
    public String getUserEmail() {
        return sharedPreferences.getString(Constants.KEY_USER_EMAIL, null);
    }

    // Get user role
    public String getUserRole() {
        return sharedPreferences.getString(Constants.KEY_USER_ROLE, Constants.ROLE_VIEWER);
    }

    // Clear user data (logout)
    public void logout() {
        editor.clear();
        editor.apply();
    }
}
