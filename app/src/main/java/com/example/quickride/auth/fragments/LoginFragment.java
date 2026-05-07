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
import com.example.quickride.auth.LauncherActivity;
import com.google.firebase.auth.FirebaseAuth;

public class LoginFragment extends Fragment {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnBack;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private AuthenticationActivity activity;
    private String userType;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // Get user type from arguments
        if (getArguments() != null) {
            userType = getArguments().getString("userType", "Customers");
        }

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

        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        if (mAuth.getCurrentUser() != null && !mAuth.getCurrentUser().isEmailVerified()) {
                            final int[] clickCount = {0};
                            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext())
                                    .setTitle("Email Not Verified")
                                    .setMessage("Your email address is not verified yet. Please check your Gmail inbox.\n\nDidn't receive the email?")
                                    .setPositiveButton("OK", (d, which) -> mAuth.signOut())
                                    .setNegativeButton("Resend Email", (d, which) -> {
                                        if (mAuth.getCurrentUser() != null) {
                                            mAuth.getCurrentUser().sendEmailVerification()
                                                    .addOnCompleteListener(resendTask -> {
                                                        if (resendTask.isSuccessful()) {
                                                            android.util.Log.d("LOGIN", "Resend verification email success");
                                                            Toast.makeText(getContext(), "Verification email resent!", Toast.LENGTH_SHORT).show();
                                                        } else {
                                                            Exception e = resendTask.getException();
                                                            String error = e != null ? e.getMessage() : "Unknown error";
                                                            android.util.Log.e("LOGIN", "Failed to resend email: " + error, e);
                                                            Toast.makeText(getContext(), "Failed to resend: " + error, Toast.LENGTH_LONG).show();
                                                        }
                                                        mAuth.signOut();
                                                    });
                                        }
                                    })
                                    .setCancelable(false)
                                    .create();

                            dialog.show();

                            // Secret bypass: Click the title 5 times to bypass verification (FOR TESTING ONLY)
                            int titleId = getResources().getIdentifier("alertTitle", "id", "android");
                            View titleView = dialog.findViewById(titleId);
                            if (titleView != null) {
                                titleView.setOnClickListener(v -> {
                                    clickCount[0]++;
                                    if (clickCount[0] >= 5) {
                                        Toast.makeText(getContext(), "Debug Bypass: Logging in...", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                        proceedToMain();
                                    }
                                });
                            }
                            return;
                        }

                        proceedToMain();
                    } else {
                        Exception e = task.getException();
                        String error = e != null ? e.getMessage() : "Login failed";
                        android.util.Log.e("LOGIN", "Login failed: " + error, e);
                        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void proceedToMain() {
        Toast.makeText(getContext(), "Login successful", Toast.LENGTH_SHORT).show();
        // Go to LauncherActivity which will redirect to correct map
        Intent intent = new Intent(getActivity(), LauncherActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
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