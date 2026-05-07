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
    private String userType;
    private String action;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        // Get data from intent
        userType = getIntent().getStringExtra("userType");
        action = getIntent().getStringExtra("action");

        if (userType == null) userType = "Customers";

        fragmentManager = getSupportFragmentManager();

        // Check if user is already logged in (should not happen here)
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already logged in, going to LauncherActivity");
            startActivity(new Intent(AuthenticationActivity.this, LauncherActivity.class));
            finish();
            return;
        }

        // Show the menu fragment with login/register options
        showMenu();
    }

    public void showLogin() {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();
        args.putString("userType", userType);
        fragment.setArguments(args);
        loadFragment(fragment, "LoginFragment", true);
    }

    public void showRegister() {
        RegisterFragment fragment = new RegisterFragment();
        Bundle args = new Bundle();
        args.putString("userType", userType);
        fragment.setArguments(args);
        loadFragment(fragment, "RegisterFragment", true);
    }

    public void showMenu() {
        MenuFragment fragment = new MenuFragment();
        loadFragment(fragment, "MenuFragment", false);
    }

    private void loadFragment(Fragment fragment, String tag, boolean addToBackStack) {
        FragmentTransaction transaction = fragmentManager.beginTransaction()
                .replace(R.id.container, fragment, tag);
        if (addToBackStack) {
            transaction.addToBackStack(tag);
        }
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            // Go back to role selection instead of exiting
            startActivity(new Intent(AuthenticationActivity.this, RoleSelectionActivity.class));
            finish();
        }
    }
}