package com.example.quickride.auth.fragments;

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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * Fragment for new user registration
 */
public class RegisterFragment extends Fragment {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister, btnBack;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private AuthenticationActivity activity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

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
        databaseRef = FirebaseDatabase.getInstance().getReference();
    }

    private void attemptRegistration() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validate inputs
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

        // Show loading
        showLoading(true);

        // Create user in Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Registration successful, save user data to database
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

        // Default to Customers (can be changed in DetailsActivity)
        String userType = "Customers";

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("profileImageUrl", "default");
        userMap.put("createdAt", System.currentTimeMillis());

        databaseRef.child("Users").child(userType).child(userId)
                .setValue(userMap)
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Registration successful", Toast.LENGTH_SHORT).show();
                        // Auth listener in Activity will handle redirect
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