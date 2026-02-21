package com.example.quickride;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CustomerLoginActivity extends AppCompatActivity {

    private EditText mEmail, mPassword;
    private Button mLogin, mRegistration;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    private boolean isTransitioning = false; // Prevents the activity from starting twice

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login);

        mAuth = FirebaseAuth.getInstance();

        // 1. Auth Listener: Handles automatic login and redirect after successful click
        firebaseAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null && !isTransitioning) {
                goToMap();
            }
        };

        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mLogin = findViewById(R.id.login);
        mRegistration = findViewById(R.id.registration);

        mRegistration.setOnClickListener(v -> {
            final String email = mEmail.getText().toString().trim();
            final String password = mPassword.getText().toString().trim();

            if (validate(email, password)) {
                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        // Path: Users -> Customers -> [UID]
                        DatabaseReference currentUserDb = FirebaseDatabase.getInstance()
                                .getReference().child("Users").child("Customers").child(userId);

                        // Set value to true (or a Map of user details)
                        currentUserDb.setValue(true).addOnFailureListener(e ->
                                Toast.makeText(CustomerLoginActivity.this, "DB Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    } else {
                        Toast.makeText(CustomerLoginActivity.this, "Registration Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        mLogin.setOnClickListener(v -> {
            final String email = mEmail.getText().toString().trim();
            final String password = mPassword.getText().toString().trim();

            if (validate(email, password)) {
                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(CustomerLoginActivity.this, "Login Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private boolean validate(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            mEmail.setError("Email is required");
            return false;
        }
        if (password.length() < 6) {
            mPassword.setError("Password must be at least 6 characters");
            return false;
        }
        return true;
    }

    // Helper method to handle navigation and clear the backstack
    private void goToMap() {
        isTransitioning = true;
        Intent intent = new Intent(CustomerLoginActivity.this, CustomerMapActivity.class);
        // This prevents the user from going "back" to the login screen
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