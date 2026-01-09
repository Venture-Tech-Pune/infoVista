package com.example.infovista.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import com.example.infovista.R;
import com.example.infovista.utils.SharedPrefManager;

public class SplashActivity extends AppCompatActivity {
    
    private static final int SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Check login status after delay
        new Handler().postDelayed(() -> {
            SharedPrefManager prefManager = SharedPrefManager.getInstance(this);
            
            Intent intent;
            if (prefManager.isLoggedIn()) {
                // User is logged in, go to dashboard
                intent = new Intent(SplashActivity.this, DashboardActivity.class);
            } else {
                // User not logged in, go to login
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            
            startActivity(intent);
            finish();
        }, SPLASH_DELAY);
    }
}
