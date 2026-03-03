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

/**
 * Fragment for user login with email and password
 */
public class LoginFragment extends Fragment {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnBack;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private AuthenticationActivity activity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        initializeViews(view);
        setupFirebase();

        return view;
    }

    private void initializeViews(View view) {
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnBack = view.findViewById(R.id.btnBack);
        progressBar = view.findViewById(R.id.progressBar);

        if (getActivity() instanceof AuthenticationActivity) {
            activity = (AuthenticationActivity) getActivity();
        }

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnBack.setOnClickListener(v -> {
            if (activity != null) {
                activity.showMenu();
            }
        });
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate inputs
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

        // Show loading
        showLoading(true);

        // Attempt login
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Login successful", Toast.LENGTH_SHORT).show();
                        // Auth listener in Activity will handle redirect
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Login failed";
                        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnLogin.setEnabled(!show);
        btnBack.setEnabled(!show);
        etEmail.setEnabled(!show);
        etPassword.setEnabled(!show);
    }
}