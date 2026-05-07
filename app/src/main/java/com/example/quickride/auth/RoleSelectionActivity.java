package com.example.quickride.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quickride.R;

public class RoleSelectionActivity extends AppCompatActivity {

    private Button btnDriver, btnCustomer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        btnDriver = findViewById(R.id.btnDriver);
        btnCustomer = findViewById(R.id.btnCustomer);

        btnDriver.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, AuthenticationActivity.class);
            intent.putExtra("userType", "Drivers");
            intent.putExtra("action", "login"); // Specify we want login/register
            startActivity(intent);
            finish(); // Finish this activity so user can't come back
        });

        btnCustomer.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, AuthenticationActivity.class);
            intent.putExtra("userType", "Customers");
            intent.putExtra("action", "login");
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // Optional: Go to LauncherActivity or exit
        super.onBackPressed();
    }
}