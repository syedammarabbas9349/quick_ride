package com.example.quickride.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.quickride.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity for collecting additional user details after registration
 * Allows user to choose role: Driver or Customer
 */
public class DetailsActivity extends AppCompatActivity implements View.OnClickListener {

    private TextInputLayout tilName;
    private TextInputEditText etName;
    private MaterialRadioButton rbDriver, rbCustomer;
    private MaterialButton btnRegister;
    private CircularProgressIndicator progressBar;
    private Toolbar toolbar;

    private FirebaseAuth mAuth;
    private String userType = "Customers"; // Default to Customer

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        initializeViews();
        setupToolbar();
        setupFirebase();
        setupListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tilName = findViewById(R.id.tilName);
        etName = findViewById(R.id.etName);
        rbDriver = findViewById(R.id.rbDriver);
        rbCustomer = findViewById(R.id.rbCustomer);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);

        // Set default selection
        rbCustomer.setChecked(true);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.complete_profile);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, R.string.error_not_logged_in, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(this);

        rbDriver.setOnClickListener(v -> {
            userType = "Drivers";
            rbDriver.setChecked(true);
            rbCustomer.setChecked(false);
        });

        rbCustomer.setOnClickListener(v -> {
            userType = "Customers";
            rbCustomer.setChecked(true);
            rbDriver.setChecked(false);
        });
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnRegister) {
            saveUserDetails();
        }
    }

    /**
     * Save user details to Firebase Database
     */
    private void saveUserDetails() {
        String name = etName.getText().toString().trim();

        // Validate name
        if (TextUtils.isEmpty(name)) {
            tilName.setError(getString(R.string.name_required));
            return;
        } else {
            tilName.setError(null);
        }

        // Show loading
        showLoading(true);

        String userId = mAuth.getCurrentUser().getUid();

        // Create user data map
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", mAuth.getCurrentUser().getEmail());
        userMap.put("profileImageUrl", "default");
        userMap.put("createdAt", System.currentTimeMillis());
        userMap.put("phone", ""); // Will be added later in settings

        // Add driver-specific fields
        if (userType.equals("Drivers")) {
            userMap.put("car", "");
            userMap.put("vehicleType", "");
            userMap.put("licensePlate", "");
            userMap.put("rating", 5.0);
            userMap.put("totalRides", 0);
            userMap.put("isOnline", false);
            userMap.put("activated", true); // Admin approval flag
        }

        // Save to Firebase
        FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child(userType)
                .child(userId)
                .updateChildren(userMap)
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(DetailsActivity.this,
                                R.string.profile_completed, Toast.LENGTH_SHORT).show();

                        // Navigate to main activity
                        Intent intent = new Intent(DetailsActivity.this, LauncherActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : getString(R.string.error_saving_data);
                        Toast.makeText(DetailsActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnRegister.setEnabled(!show);
        etName.setEnabled(!show);
        rbDriver.setEnabled(!show);
        rbCustomer.setEnabled(!show);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}