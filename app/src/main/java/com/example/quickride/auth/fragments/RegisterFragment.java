package com.example.quickride.auth.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.quickride.R;
import com.example.quickride.auth.AuthenticationActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister, btnBack;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private AuthenticationActivity activity;
    private String userType;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        // Get user type from arguments
        if (getArguments() != null) {
            userType = getArguments().getString("userType", "Customers");
        }

        initializeViews(view);
        setupFirebase();

        return view;
    }

    private void initializeViews(View view) {
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        btnRegister = view.findViewById(R.id.btnRegister);
        btnBack = view.findViewById(R.id.btnBack);
        progressBar = view.findViewById(R.id.progressBar);

        if (getActivity() instanceof AuthenticationActivity) {
            activity = (AuthenticationActivity) getActivity();
        }

        btnRegister.setOnClickListener(v -> attemptRegistration());
        btnBack.setOnClickListener(v -> {
            if (activity != null) {
                activity.showMenu();
            }
        });
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
    }

    private void attemptRegistration() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password required");
            return;
        } else if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        showLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveUserData(name, email);
                    } else {
                        showLoading(false);
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Registration failed";
                        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserData(String name, String email) {
        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("profileImageUrl", "default");
        userMap.put("createdAt", System.currentTimeMillis());

        // Add role-specific fields
        if (userType.equals("Drivers")) {
            userMap.put("car", "");
            userMap.put("vehicleType", "");
            userMap.put("licensePlate", "");
            userMap.put("rating", 5.0);
            userMap.put("totalRides", 0);
            userMap.put("isOnline", false);
        }

        FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child(userType)
                .child(userId)
                .setValue(userMap)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Registration successful! Please login.", Toast.LENGTH_LONG).show();

                        // Sign out so user can login with new credentials
                        mAuth.signOut();

                        // Go back to login fragment
                        if (activity != null) {
                            activity.showLogin();
                        }
                    } else {
                        Toast.makeText(getContext(), "Error saving user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnRegister.setEnabled(!show);
        btnBack.setEnabled(!show);
        etName.setEnabled(!show);
        etEmail.setEnabled(!show);
        etPassword.setEnabled(!show);
        etConfirmPassword.setEnabled(!show);
    }
}