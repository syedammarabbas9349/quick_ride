package com.example.quickride.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quickride.R;
import com.example.quickride.auth.AuthenticationActivity;
import com.example.quickride.auth.DetailsActivity;
import com.example.quickride.customer.CustomerMapActivity;
import com.example.quickride.driver.DriverMapActivity;
import com.example.quickride.utils.NotificationHelper;
import com.example.quickride.utils.PaymentHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Launcher/Splash Activity
 *
 * Responsible for:
 * - Showing splash screen
 * - Checking if user is logged in
 * - Determining user type (Customer/Driver)
 * - Redirecting to appropriate activity
 * - Initializing APIs (Notifications, Payments)
 */
public class LauncherActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1500; // 1.5 seconds splash delay
    private int userTypeCheckCount = 0;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        mAuth = FirebaseAuth.getInstance();

        // Show splash for a moment then check user state
        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserState, SPLASH_DELAY);
    }

    /**
     * Check if user is logged in and determine next action
     */
    private void checkUserState() {
        if (mAuth.getCurrentUser() != null) {
            // User is logged in, determine their type
            currentUserId = mAuth.getCurrentUser().getUid();
            checkUserType();
        } else {
            // No user logged in, go to authentication
            navigateToAuthentication();
        }
    }

    /**
     * Check if user is a Customer or Driver
     */
    private void checkUserType() {
        // Check if user exists in Customers node
        DatabaseReference customerRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child("Customers")
                .child(currentUserId);

        customerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // User is a customer
                    initializeApis("Customers");
                    navigateToCustomerMap();
                } else {
                    checkDriverType();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                checkDriverType();
            }
        });
    }

    /**
     * Check if user exists in Drivers node
     */
    private void checkDriverType() {
        DatabaseReference driverRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child("Drivers")
                .child(currentUserId);

        driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // User is a driver
                    initializeApis("Drivers");
                    navigateToDriverMap();
                } else {
                    // User exists in Auth but has no profile - needs to complete registration
                    userTypeCheckCount++;
                    if (userTypeCheckCount >= 2) {
                        navigateToDetails();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                userTypeCheckCount++;
                if (userTypeCheckCount >= 2) {
                    navigateToDetails();
                }
            }
        });
    }

    /**
     * Initialize third-party APIs
     * @param userType - "Customers" or "Drivers"
     */
    private void initializeApis(String userType) {
        // Initialize OneSignal for push notifications
        NotificationHelper.getInstance(this).initialize(userType, currentUserId);

        // Initialize payment configuration (for future use)
        PaymentHelper.initialize(this);
    }

    /**
     * Navigate to Customer Map
     */
    private void navigateToCustomerMap() {
        Intent intent = new Intent(LauncherActivity.this, CustomerMapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Navigate to Driver Map
     */
    private void navigateToDriverMap() {
        Intent intent = new Intent(LauncherActivity.this, DriverMapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Navigate to Authentication (Login/Register)
     */
    private void navigateToAuthentication() {
        Intent intent = new Intent(LauncherActivity.this, AuthenticationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Navigate to Details (Complete Profile)
     */
    private void navigateToDetails() {
        Intent intent = new Intent(LauncherActivity.this, DetailsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}