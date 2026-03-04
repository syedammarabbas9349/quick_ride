package com.example.quickride.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quickride.R;
import com.example.quickride.auth.AuthenticationActivity;
import com.example.quickride.auth.DetailsActivity;
import com.example.quickride.customer.CustomerMapActivity;
import com.example.quickride.driver.DriverMapActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LauncherActivity extends AppCompatActivity {

    private static final String TAG = "LauncherActivity";
    private static final int SPLASH_DELAY = 1500;
    private int userTypeCheckCount = 0;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Log.d(TAG, "onCreate started");
            setContentView(R.layout.activity_launcher);
            Log.d(TAG, "Layout set successfully");

            mAuth = FirebaseAuth.getInstance();

            new Handler(Looper.getMainLooper()).postDelayed(this::checkUserState, SPLASH_DELAY);

        } catch (Exception e) {
            Log.e(TAG, "Crash in onCreate: " + e.getMessage(), e);
        }
    }

    private void checkUserState() {
        try {
            Log.d(TAG, "checkUserState started");

            if (mAuth == null) {
                Log.e(TAG, "mAuth is null");
                navigateToAuthentication();
                return;
            }

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                Log.d(TAG, "User is logged in: " + currentUser.getUid());
                checkUserType(currentUser.getUid());
            } else {
                Log.d(TAG, "No user logged in");
                navigateToAuthentication();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in checkUserState: " + e.getMessage(), e);
            navigateToAuthentication();
        }
    }

    private void checkUserType(String userId) {
        Log.d(TAG, "checkUserType for userId: " + userId);
        userTypeCheckCount = 0;

        // Check if user exists in Customers node
        DatabaseReference customerRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child("Customers")
                .child(userId);

        customerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d(TAG, "User is a customer");
                    navigateToCustomerMap();
                } else {
                    checkDriverType(userId);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Customer check cancelled: " + databaseError.getMessage());
                checkDriverType(userId);
            }
        });
    }

    private void checkDriverType(String userId) {
        DatabaseReference driverRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child("Drivers")
                .child(userId);

        driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d(TAG, "User is a driver");
                    navigateToDriverMap();
                } else {
                    userTypeCheckCount++;
                    Log.d(TAG, "User has no profile, checkCount=" + userTypeCheckCount);
                    if (userTypeCheckCount >= 1) {
                        Log.d(TAG, "Navigating to DetailsActivity");
                        navigateToDetails();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Driver check cancelled: " + databaseError.getMessage());
                userTypeCheckCount++;
                if (userTypeCheckCount >= 1) {
                    navigateToDetails();
                }
            }
        });
    }

    private void navigateToCustomerMap() {
        Log.d(TAG, "navigateToCustomerMap");
        Intent intent = new Intent(LauncherActivity.this, CustomerMapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToDriverMap() {
        Log.d(TAG, "navigateToDriverMap");
        Intent intent = new Intent(LauncherActivity.this, DriverMapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToAuthentication() {
        Log.d(TAG, "navigateToAuthentication");
        Intent intent = new Intent(LauncherActivity.this, AuthenticationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToDetails() {
        Log.d(TAG, "navigateToDetails");
        Intent intent = new Intent(LauncherActivity.this, DetailsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}