package com.example.quickride;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverLoginActivity extends AppCompatActivity {
    private EditText mEmail, mPassword;
    private Button mLogin, mRegistration;
    private ProgressBar mProgressBar;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    private boolean isTransitioning = false; // Prevents double-start of Activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        mAuth = FirebaseAuth.getInstance();

        // 1. Initialize Views - Ensure these IDs match your activity_driver_login.xml
        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mLogin = findViewById(R.id.login);
        mRegistration = findViewById(R.id.registration);
        mProgressBar = findViewById(R.id.progressBar); // Added fix for potential NullPointerException

        // 2. AuthStateListener
        firebaseAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null && !isTransitioning) {
                goToMap();
            }
        };

        // 3. Registration Logic
        mRegistration.setOnClickListener(v -> {
            final String email = mEmail.getText().toString().trim();
            final String password = mPassword.getText().toString().trim();

            if (validateInputs(email, password)) {
                showLoading(true);
                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        DatabaseReference currentUserDb = FirebaseDatabase.getInstance()
                                .getReference().child("Users").child("Drivers").child(userId);

                        currentUserDb.setValue(true).addOnCompleteListener(dbTask -> {
                            showLoading(false);
                            if (dbTask.isSuccessful()) {
                                goToMap();
                            } else {
                                Toast.makeText(DriverLoginActivity.this, "DB Error: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        showLoading(false);
                        Toast.makeText(DriverLoginActivity.this, "Reg Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        // 4. Login Logic
        mLogin.setOnClickListener(v -> {
            final String email = mEmail.getText().toString().trim();
            final String password = mPassword.getText().toString().trim();

            if (validateInputs(email, password)) {
                showLoading(true);
                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                    // We don't call showLoading(false) here if successful because goToMap() finishes the activity
                    if (!task.isSuccessful()) {
                        showLoading(false);
                        Toast.makeText(DriverLoginActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private boolean validateInputs(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            mEmail.setError("Email required");
            return false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            mPassword.setError("Password must be at least 6 characters");
            return false;
        }
        return true;
    }

    private void showLoading(boolean isLoading) {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        mLogin.setEnabled(!isLoading);
        mRegistration.setEnabled(!isLoading);
    }

    private void goToMap() {
        if (isTransitioning) return;
        isTransitioning = true;

        Intent intent = new Intent(DriverLoginActivity.this, DriverMapActivity.class);
        // This clears the Activity stack so the user can't "back button" into the login screen
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (firebaseAuthListener != null) {
            mAuth.removeAuthStateListener(firebaseAuthListener);
        }
    }
}