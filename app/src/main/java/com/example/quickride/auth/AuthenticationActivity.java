package com.example.quickride.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.quickride.R;
import com.example.quickride.auth.fragments.LoginFragment;
import com.example.quickride.auth.fragments.MenuFragment;
import com.example.quickride.auth.fragments.RegisterFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthenticationActivity extends AppCompatActivity {

    private static final String TAG = "AuthActivity";
    private FragmentManager fragmentManager;
    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        fragmentManager = getSupportFragmentManager();
        setupAuthListener();

        // Check if user is already logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already logged in: " + currentUser.getUid());
            startActivity(new Intent(AuthenticationActivity.this, LauncherActivity.class));
            finish();
        } else {
            loadFragment(new MenuFragment(), "MenuFragment", false);
        }
    }

    private void setupAuthListener() {
        authStateListener = firebaseAuth -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && !isFinishing()) {
                Log.d(TAG, "User logged in, redirecting");
                startActivity(new Intent(AuthenticationActivity.this, LauncherActivity.class));
                finish();
            }
        };
    }

    public void loadFragment(Fragment fragment, String tag, boolean addToBackStack) {
        FragmentTransaction transaction = fragmentManager.beginTransaction()
                .replace(R.id.container, fragment, tag);
        if (addToBackStack) {
            transaction.addToBackStack(tag);
        }
        transaction.commit();
    }

    public void showLogin() {
        loadFragment(new LoginFragment(), "LoginFragment", true);
    }

    public void showRegistration() {
        loadFragment(new RegisterFragment(), "RegisterFragment", true);
    }

    public void showMenu() {
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        loadFragment(new MenuFragment(), "MenuFragment", false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
    }
}