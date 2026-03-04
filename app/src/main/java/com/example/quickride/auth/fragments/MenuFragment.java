package com.example.quickride.auth.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;  // Changed from MaterialButton

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.quickride.R;
import com.example.quickride.auth.AuthenticationActivity;

/**
 * Welcome fragment with options to login or register
 */
public class MenuFragment extends Fragment {

    private Button btnLogin, btnRegister;  // Changed to Button
    private AuthenticationActivity activity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        initializeViews(view);
        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        btnLogin = view.findViewById(R.id.btnLogin);
        btnRegister = view.findViewById(R.id.btnRegister);

        if (getActivity() instanceof AuthenticationActivity) {
            activity = (AuthenticationActivity) getActivity();
        }
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            if (activity != null) {
                activity.showLogin();
            }
        });

        btnRegister.setOnClickListener(v -> {
            if (activity != null) {
                activity.showRegistration();
            }
        });
    }
}