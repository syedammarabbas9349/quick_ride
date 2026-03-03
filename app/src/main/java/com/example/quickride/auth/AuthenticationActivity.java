package com.example.quickride.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.quickride.R;
import com.example.quickride.auth.fragments.LoginFragment;
import com.example.quickride.auth.fragments.MenuFragment;
import com.example.quickride.auth.fragments.RegisterFragment;
import com.google.android.material.transition.MaterialSharedAxis;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Activity that manages authentication fragments:
 * - MenuFragment (Welcome screen)
 * - LoginFragment (User login)
 * - RegisterFragment (New user registration)
 *
 * Handles navigation between fragments and automatic login redirect
 */
public class AuthenticationActivity extends AppCompatActivity {

    private FragmentManager fragmentManager;
    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        fragmentManager = getSupportFragmentManager();

        // Listen for authentication state changes
        setupAuthListener();

        // Load the initial menu fragment
        loadFragment(new MenuFragment(), "MenuFragment", false);
    }

    /**
     * Sets up Firebase Auth state listener to automatically redirect
     * logged-in users to the main activity
     */
    private void setupAuthListener() {
        authStateListener = firebaseAuth -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                // User is signed in, redirect to main activity
                Intent intent = new Intent(AuthenticationActivity.this, LauncherActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        };
    }

    /**
     * Loads a fragment with Material transition animations
     *
     * @param fragment The fragment to load
     * @param tag Fragment tag for back stack
     * @param addToBackStack Whether to add to back stack
     */
    public void loadFragment(Fragment fragment, String tag, boolean addToBackStack) {
        // Set up Material transitions
        fragment.setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        fragment.setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
        fragment.setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        fragment.setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));

        FragmentTransaction transaction = fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.container, fragment, tag);

        if (addToBackStack) {
            transaction.addToBackStack(tag);
        }

        transaction.commit();
    }

    /**
     * Navigates to the registration fragment
     */
    public void showRegistration() {
        loadFragment(new RegisterFragment(), "RegisterFragment", true);
    }

    /**
     * Navigates to the login fragment
     */
    public void showLogin() {
        loadFragment(new LoginFragment(), "LoginFragment", true);
    }

    /**
     * Navigates back to the menu fragment
     */
    public void showMenu() {
        // Clear back stack and go to menu
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        loadFragment(new MenuFragment(), "MenuFragment", false);
    }

    @Override
    public void onBackPressed() {
        // Handle back navigation between fragments
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Add auth listener when activity starts
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Remove auth listener when activity stops to prevent memory leaks
        if (authStateListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener);
        }
    }
}