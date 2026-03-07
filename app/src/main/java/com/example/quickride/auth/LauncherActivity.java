package com.example.quickride.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quickride.R;
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
    private static final int SPLASH_DELAY = 1000;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_launcher);
            Log.d(TAG, "Layout inflated successfully");

            mAuth = FirebaseAuth.getInstance();
            Log.d(TAG, "FirebaseAuth initialized");

            new Handler(Looper.getMainLooper()).postDelayed(this::checkUserState, SPLASH_DELAY);

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void checkUserState() {
        try {
            Log.d(TAG, "checkUserState started");

            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String uid = currentUser.getUid();
                Log.d(TAG, "User logged in: " + uid);
                Log.d(TAG, "User email: " + currentUser.getEmail());
                checkUserRole(uid);
            } else {
                Log.d(TAG, "No user logged in, going to role selection");
                startActivity(new Intent(LauncherActivity.this, RoleSelectionActivity.class));
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in checkUserState: " + e.getMessage(), e);
            goToRoleSelection();
        }
    }

    private void checkUserRole(String userId) {
        Log.d(TAG, "checkUserRole for userId: " + userId);

        // First check if user is a driver
        DatabaseReference driverRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child("Drivers")
                .child(userId);

        driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                try {
                    Log.d(TAG, "Driver snapshot exists: " + snapshot.exists());

                    if (snapshot.exists()) {
                        Log.d(TAG, "User is a driver, navigating to DriverMapActivity");
                        Intent intent = new Intent(LauncherActivity.this, DriverMapActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.d(TAG, "Not a driver, checking if customer");
                        checkIfCustomer(userId);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in driver onDataChange: " + e.getMessage(), e);
                    goToRoleSelection();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Driver check cancelled: " + error.getMessage());
                goToRoleSelection();
            }
        });
    }

    private void checkIfCustomer(String userId) {
        Log.d(TAG, "checkIfCustomer for userId: " + userId);

        DatabaseReference customerRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child("Customers")
                .child(userId);

        customerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                try {
                    Log.d(TAG, "Customer snapshot exists: " + snapshot.exists());

                    if (snapshot.exists()) {
                        Log.d(TAG, "User is a customer, navigating to CustomerMapActivity");
                        Intent intent = new Intent(LauncherActivity.this, CustomerMapActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        // This should not happen - user has no role
                        Log.e(TAG, "User has no role in database!");
                        Toast.makeText(LauncherActivity.this,
                                "User profile not found. Please register again.", Toast.LENGTH_LONG).show();
                        goToRoleSelection();
                    }
                    finish();

                } catch (Exception e) {
                    Log.e(TAG, "Error in customer onDataChange: " + e.getMessage(), e);
                    goToRoleSelection();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Customer check cancelled: " + error.getMessage());
                goToRoleSelection();
            }
        });
    }

    private void goToRoleSelection() {
        Log.d(TAG, "Going to RoleSelectionActivity");
        Intent intent = new Intent(LauncherActivity.this, RoleSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
    }
}