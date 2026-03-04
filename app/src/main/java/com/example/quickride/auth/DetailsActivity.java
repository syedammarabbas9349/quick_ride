package com.example.quickride.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.quickride.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class DetailsActivity extends AppCompatActivity {

    private EditText etName;
    private RadioGroup radioGroup;
    private RadioButton rbDriver, rbCustomer;
    private Button btnContinue;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        initializeViews();
        setupToolbar();
        setupListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        etName = findViewById(R.id.etName);
        radioGroup = findViewById(R.id.radioGroup);
        rbDriver = findViewById(R.id.rbDriver);
        rbCustomer = findViewById(R.id.rbCustomer);
        btnContinue = findViewById(R.id.btnContinue);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Complete Profile");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnContinue.setOnClickListener(v -> saveUserDetails());
    }

    private void saveUserDetails() {
        String name = etName.getText().toString().trim();
        String userType = rbDriver.isChecked() ? "Drivers" : "Customers";

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            return;
        }

        showLoading(true);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", FirebaseAuth.getInstance().getCurrentUser().getEmail());
        userMap.put("profileImageUrl", "default");
        userMap.put("createdAt", System.currentTimeMillis());

        // Add driver-specific fields if needed
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
                .updateChildren(userMap)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Profile completed!", Toast.LENGTH_SHORT).show();
                    // Go to LauncherActivity which will redirect to correct map
                    startActivity(new Intent(this, LauncherActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnContinue.setEnabled(!show);
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