package com.example.quickride;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // We'll create this layout

        mAuth = FirebaseAuth.getInstance();

        // 1-second delay for splash screen
        new Handler().postDelayed(() -> {
            FirebaseUser user = mAuth.getCurrentUser();

            if (user != null) {
                // User is logged in → go to Home
                startActivity(new Intent(SplashActivity.this, HomeActivity.class));
            } else {
                // User is not logged in → go to MainActivity (welcome screen)
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            }
            finish(); // Close splash so user can't go back
        }, 1000);
    }
}