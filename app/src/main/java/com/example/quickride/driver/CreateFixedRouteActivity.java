package com.example.quickride.driver;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import android.widget.Button;
import com.example.quickride.R;
import com.example.quickride.models.FixedRoute;
import com.google.android.material.button.MaterialButton;
import android.widget.EditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CreateFixedRouteActivity extends AppCompatActivity {

    private EditText etStartPoint, etDestination, etWaypoints, etTimeWindow, etSeats, etFare, etDistance;
    private SwitchCompat switchActive;
    private Button btnSaveRoute;
    private DatabaseReference fixedRouteRef;
    private com.example.quickride.utils.RouteHelper routeHelper;
    private String driverId;

    private String driverName = "";
    private String driverImageUrl = "default";
    private double driverRating = 5.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_fixed_route);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Create Fixed Route");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etStartPoint = findViewById(R.id.etStartPoint);
        etDestination = findViewById(R.id.etDestination);
        etWaypoints = findViewById(R.id.etWaypoints);
        etTimeWindow = findViewById(R.id.etTimeWindow);
        etSeats = findViewById(R.id.etSeats);
        etFare = findViewById(R.id.etFare);
        etDistance = findViewById(R.id.etDistance);
        switchActive = findViewById(R.id.switchActive);
        btnSaveRoute = findViewById(R.id.btnSaveRoute);

        driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        fixedRouteRef = FirebaseDatabase.getInstance().getReference().child("FixedRoutes");

        routeHelper = new com.example.quickride.utils.RouteHelper(this, getString(R.string.google_maps_key));
        setupDistanceCalculation();

        loadDriverInfo();
        // loadExistingRoute(); // Removed to support multiple routes

        btnSaveRoute.setOnClickListener(v -> saveRoute());
    }

    private void setupDistanceCalculation() {
        routeHelper.setCallback(new com.example.quickride.utils.RouteHelper.RouteCallback() {
            @Override
            public void onRouteSuccess(java.util.ArrayList<com.google.android.gms.maps.model.LatLng> path, double distance, int duration) {
                etDistance.setText(String.format(java.util.Locale.getDefault(), "%.1f", distance));
            }

            @Override
            public void onRouteFailure(String error) {
                // Silently fail or log
                android.util.Log.e("FixedRoute", "Distance calc failed: " + error);
            }
        });

        android.view.View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            if (!hasFocus) {
                calculateDistanceAutomatically();
            }
        };

        etStartPoint.setOnFocusChangeListener(focusListener);
        etDestination.setOnFocusChangeListener(focusListener);
    }

    private void calculateDistanceAutomatically() {
        String start = etStartPoint.getText().toString().trim();
        String dest = etDestination.getText().toString().trim();

        if (!start.isEmpty() && !dest.isEmpty()) {
            etDistance.setHint("Calculating...");
            routeHelper.getRoute(start, dest);
        }
    }


    private void loadDriverInfo() {
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId);
        driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.child("name").exists()) {
                        driverName = snapshot.child("name").getValue().toString();
                    }
                    if (snapshot.child("profileImageUrl").exists()) {
                        driverImageUrl = snapshot.child("profileImageUrl").getValue().toString();
                    }
                    if (snapshot.child("rating").exists()) {
                        driverRating = snapshot.child("rating").getValue(Double.class);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void loadExistingRoute() {
        fixedRouteRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.child("startPoint").exists()) etStartPoint.setText(snapshot.child("startPoint").getValue().toString());
                    if (snapshot.child("destination").exists()) etDestination.setText(snapshot.child("destination").getValue().toString());
                    if (snapshot.child("waypoints").exists()) etWaypoints.setText(snapshot.child("waypoints").getValue().toString());
                    if (snapshot.child("departureTimeWindow").exists()) etTimeWindow.setText(snapshot.child("departureTimeWindow").getValue().toString());
                    if (snapshot.child("totalSeats").exists()) etSeats.setText(snapshot.child("totalSeats").getValue().toString());
                    if (snapshot.child("fixedFare").exists()) etFare.setText(snapshot.child("fixedFare").getValue().toString());
                    if (snapshot.child("active").exists()) {
                        Boolean isActive = snapshot.child("active").getValue(Boolean.class);
                        if (isActive != null) {
                            switchActive.setChecked(isActive);
                        }
                    }
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle("Manage Fixed Route");
                        getSupportActionBar().setSubtitle("Editing your active route");
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void saveRoute() {
        String startPoint = etStartPoint.getText().toString().trim();
        String destination = etDestination.getText().toString().trim();
        String waypoints = etWaypoints.getText().toString().trim();
        String timeWindow = etTimeWindow.getText().toString().trim();
        String seatsStr = etSeats.getText().toString().trim();
        String fareStr = etFare.getText().toString().trim();
        String distanceStr = etDistance.getText().toString().trim();

        if (startPoint.isEmpty() || destination.isEmpty() || timeWindow.isEmpty() || seatsStr.isEmpty() || fareStr.isEmpty() || distanceStr.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int seats = Integer.parseInt(seatsStr);
        double fare = Double.parseDouble(fareStr);
        double distance = Double.parseDouble(distanceStr);

        String routeId = fixedRouteRef.push().getKey();
        if (routeId == null) routeId = java.util.UUID.randomUUID().toString();
        
        FixedRoute route = new FixedRoute();
        route.setRouteId(routeId);
        route.setDriverId(driverId);
        route.setDriverName(driverName);
        route.setDriverImageUrl(driverImageUrl);
        route.setRating(driverRating);
        route.setStartPoint(startPoint);
        route.setDestination(destination);
        route.setWaypoints(waypoints);
        route.setDepartureTimeWindow(timeWindow);
        route.setTotalSeats(seats);
        route.setAvailableSeats(seats);
        route.setFixedFare(fare);
        route.setDistance(distance);
        route.setActive(switchActive.isChecked());
        route.setCreatedAt(System.currentTimeMillis());

        fixedRouteRef.child(routeId).setValue(route.toMap()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "New route added successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error saving route", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
