package com.example.quickride;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    TextView tvWelcome;
    Button btnBookRide;
    ImageView profileIcon, notificationIcon, btnLogout; // Added btnLogout
    FloatingActionButton fabCurrentLocation;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide action bar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_home);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Views
        tvWelcome = findViewById(R.id.tvWelcome);
        btnBookRide = findViewById(R.id.btnBookRide);
        profileIcon = findViewById(R.id.profileIcon);
        notificationIcon = findViewById(R.id.notificationIcon);
        btnLogout = findViewById(R.id.btnLogout); // NEW
        fabCurrentLocation = findViewById(R.id.fabCurrentLocation);

        // Show logged-in user's email
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            String greeting = getGreeting();

            // Show email in welcome text
            tvWelcome.setText(greeting + "\n" + email);
        }

        // Book Ride Button
        btnBookRide.setOnClickListener(v -> {
            Toast.makeText(HomeActivity.this, "Finding your ride...", Toast.LENGTH_SHORT).show();
        });

        // Profile Icon - Open Profile
        profileIcon.setOnClickListener(v -> {
            Toast.makeText(HomeActivity.this, "Profile clicked", Toast.LENGTH_SHORT).show();
        });

        // Notification Icon
        notificationIcon.setOnClickListener(v -> {
            Toast.makeText(HomeActivity.this, "Notifications", Toast.LENGTH_SHORT).show();
        });

        // NEW: Logout Button Click
        btnLogout.setOnClickListener(v -> {
            // Show confirmation dialog
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Perform logout
                        mAuth.signOut();
                        Toast.makeText(HomeActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();

                        // Go to MainActivity (welcome screen)
                        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        // Current Location FAB
        fabCurrentLocation.setOnClickListener(v -> {
            Toast.makeText(HomeActivity.this, "Centering on your location", Toast.LENGTH_SHORT).show();
        });
    }

    // Helper method to get time-based greeting
    private String getGreeting() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);

        if (hour < 12) {
            return "Good morning!";
        } else if (hour < 17) {
            return "Good afternoon!";
        } else {
            return "Good evening!";
        }
    }

    // Handle back button press
    @Override
    public void onBackPressed() {
        // Ask user to confirm exit
        new android.app.AlertDialog.Builder(this)
                .setTitle("Exit QuickRide")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
    }
}