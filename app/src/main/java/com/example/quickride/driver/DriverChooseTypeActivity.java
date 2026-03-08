package com.example.quickride.driver;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickride.R;
import com.example.quickride.adapters.TypeAdapter;
import com.example.quickride.models.ServiceType;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverChooseTypeActivity extends AppCompatActivity {

    private TypeAdapter typeAdapter;
    private final List<ServiceType> typeList = new ArrayList<>();
    private Button btnConfirm;
    private RecyclerView recyclerView;
    private Toolbar toolbar;

    private DatabaseReference driverRef;
    private String currentDriverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_choose_type);

        initializeViews();
        setupToolbar();
        setupFirebase();
        loadVehicleTypes();
        setupRecyclerView();
        checkExistingSelection();
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
        btnConfirm = findViewById(R.id.btnConfirm);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.choose_vehicle_type);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupFirebase() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        currentDriverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        driverRef = FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child("Drivers")
                .child(currentDriverId);
    }

    private void loadVehicleTypes() {
        // Create vehicle types list
        typeList.add(new ServiceType("economy", "Economy", "economy", 15.0, 4, R.drawable.ic_economy_car));
        typeList.add(new ServiceType("premium", "Premium", "premium", 25.0, 4, R.drawable.ic_premium_car));
        typeList.add(new ServiceType("xl", "XL", "xl", 35.0, 6, R.drawable.ic_suv));
        typeList.add(new ServiceType("bike", "Bike", "bike", 10.0, 1, R.drawable.ic_bike));
    }

    private void setupRecyclerView() {
        typeAdapter = new TypeAdapter(typeList, this, null, (type, position) -> {
            // Handle type selection if needed
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(typeAdapter);
    }

    private void checkExistingSelection() {
        // Check if driver already has a selected vehicle type
        String selectedType = getIntent().getStringExtra("vehicleType");
        if (selectedType != null && !selectedType.isEmpty()) {
            for (int i = 0; i < typeList.size(); i++) {
                if (typeList.get(i).getVehicleType().equals(selectedType)) {
                    typeAdapter.setSelectedItem(typeList.get(i));
                    recyclerView.scrollToPosition(i);
                    break;
                }
            }
        }
    }

    private void setupClickListeners() {
        btnConfirm.setOnClickListener(v -> saveVehicleType());
    }

    private void saveVehicleType() {

        ServiceType selected = typeAdapter.getSelectedItem();

        if (selected == null) {
            Toast.makeText(this, R.string.select_vehicle_type, Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare data to save
        Map<String, Object> vehicleData = new HashMap<>();
        vehicleData.put("vehicleType", selected.getVehicleType());
        vehicleData.put("vehicleName", selected.getName());
        vehicleData.put("pricePerKm", selected.getPricePerKm());
        vehicleData.put("capacity", selected.getCapacity());
        vehicleData.put("updatedAt", System.currentTimeMillis());
        vehicleData.put("active", true);

        // Show loading
        btnConfirm.setEnabled(false);
        btnConfirm.setText(R.string.saving);

        // Save to Firebase
        driverRef.updateChildren(vehicleData)
                .addOnSuccessListener(aVoid -> {

                    Toast.makeText(this, R.string.vehicle_type_saved, Toast.LENGTH_SHORT).show();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("vehicleType", selected.getVehicleType());
                    resultIntent.putExtra("vehicleName", selected.getName());
                    setResult(Activity.RESULT_OK, resultIntent);

                    finish();
                })
                .addOnFailureListener(e -> {

                    btnConfirm.setEnabled(true);
                    btnConfirm.setText(R.string.confirm);

                    Toast.makeText(this,
                            R.string.error_saving + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}