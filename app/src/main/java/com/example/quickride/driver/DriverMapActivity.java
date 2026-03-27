package com.example.quickride.driver;

import com.google.android.material.navigation.NavigationView;
import com.google.android.gms.maps.model.LatLng;
import android.os.Handler;
import com.example.quickride.adapters.PassengerAdapter;
import com.example.quickride.models.SharedPassenger;
import com.example.quickride.utils.SharedRideManager;
import android.view.Menu;
import android.view.MenuItem;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.quickride.R;
import com.example.quickride.adapters.DrawerAdapter;
import com.example.quickride.adapters.CardRequestAdapter;
import com.example.quickride.auth.LauncherActivity;
import com.example.quickride.history.HistoryActivity;
import com.example.quickride.models.RideRequest;
import com.example.quickride.models.User;
import com.example.quickride.payment.PayoutActivity;
import com.example.quickride.utils.NotificationHelper;
import com.example.quickride.utils.RouteHelper;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DriverMapActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        RouteHelper.RouteCallback {


    private RecyclerView passengerRecyclerView;
    private TextView tvPassengerCountHeader, tvTotalEarningsShared;
    private LinearLayout passengerListLayout;
    private Button btnFindMorePassengers;
    private PassengerAdapter passengerAdapter;    private static final String TAG = "DriverMapActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int MAX_SEARCH_DISTANCE = 5;

    private GoogleMap mMap;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private ImageView drawerButton;
    private ImageView customerProfileImage;
    private Switch workingSwitch;
    private Button rideStatusButton;
    private Button fabMaps, fabCall;
    private Button cancelButton;
    private TextView customerName, pickupAddress, driverNameHeader, driverStatusHeader;
    private LinearLayout customerInfo, bringUpBottomLayout;
    private LinearLayout bottomSheet;
    private RecyclerView requestsRecyclerView;
    private TextView noRequestsText;

    // Bottom Sheet
    private View bottomSheetView;
    private BottomSheetBehavior<View> bottomSheetBehavior;

    // Data
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private Location lastLocation;
    private User currentDriver;
    private RideRequest currentRide;
    private List<RideRequest> requestList = new ArrayList<>();
    private CardRequestAdapter requestAdapter;


    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private boolean isSearching = false;

    // Firebase
    private DatabaseReference driverRef;
    private DatabaseReference rideInfoRef;
    private GeoFire geoFireWorking;
    private ValueEventListener rideStatusListener;
    private GeoQuery geoQuery;

    // Map
    private Marker pickupMarker, destinationMarker;
    private List<Polyline> polylines = new ArrayList<>();
    private boolean zoomUpdated = false;
    private boolean started = false;

    // Helpers
    private RouteHelper routeHelper;
    private NotificationHelper notificationHelper;

    // Drawer items
    private String[] drawerItems;
    private int[] drawerIcons = {
            R.drawable.ic_history_24dp,
            R.drawable.ic_earnings_24dp,
            R.drawable.ic_settings_24dp,
            R.drawable.ic_help_24dp,
            R.drawable.ic_logout
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        initializeViews();
        setupToolbar();
        setupDrawerMenu();
        setupFirebase();
        setupLocation();
        setupMap();
        setupRecyclerView();
        setupBottomSheet();
        setupListeners();
        loadDriverData();
        checkForActiveRide();
        setupPassengerList();
    }

    private void initializeViews() {

        drawer = findViewById(R.id.drawer_layout);
        drawerButton = findViewById(R.id.drawerButton);
        navigationView = findViewById(R.id.navigationView);
        View headerView = navigationView.getHeaderView(0);
        driverNameHeader = headerView.findViewById(R.id.driverNameDrawer);
        driverStatusHeader = headerView.findViewById(R.id.driverStatusDrawer);
        workingSwitch = findViewById(R.id.workingSwitch);
        rideStatusButton = findViewById(R.id.rideStatus);
        fabMaps = findViewById(R.id.openMaps);
        fabCall = findViewById(R.id.phone);
        cancelButton = findViewById(R.id.cancel);
        customerInfo = findViewById(R.id.customerInfo);
        bringUpBottomLayout = findViewById(R.id.bringUpBottomLayout);
        bottomSheet = findViewById(R.id.bottomSheet);
        requestsRecyclerView = findViewById(R.id.requestsRecyclerView);
        noRequestsText = findViewById(R.id.noRequestsText);
        customerName = findViewById(R.id.customerName);
        pickupAddress = findViewById(R.id.pickupAddress);
        customerProfileImage = findViewById(R.id.customerProfileImage);
        passengerRecyclerView = findViewById(R.id.passengerRecyclerView);
        tvPassengerCountHeader = findViewById(R.id.tvPassengerCountHeader);
        tvTotalEarningsShared = findViewById(R.id.tvTotalEarningsShared);
        passengerListLayout = findViewById(R.id.passengerListLayout);
        btnFindMorePassengers = findViewById(R.id.btnFindMorePassengers);
    }

    private void setupToolbar() {
        if (drawerButton != null) {
            drawerButton.setOnClickListener(v ->
                    drawer.openDrawer(GravityCompat.START));
        }
    }
    private void showPassengerList() {
        if (currentRide == null || !currentRide.isSharingEnabled()) {
            if (passengerListLayout != null) passengerListLayout.setVisibility(View.GONE);
            return;
        }

        if (passengerListLayout != null) passengerListLayout.setVisibility(View.VISIBLE);

        if (currentRide.getPassengers() != null && !currentRide.getPassengers().isEmpty()) {
            passengerAdapter = new PassengerAdapter(currentRide.getPassengers());
            passengerRecyclerView.setAdapter(passengerAdapter);

            if (tvPassengerCountHeader != null) {
                tvPassengerCountHeader.setText("Passengers (" + currentRide.getCurrentPassengers() + "/" +
                        currentRide.getMaxPassengers() + ")");
            }

            // Calculate total earnings from all passengers
            double totalEarnings = 0;
            for (SharedPassenger p : currentRide.getPassengers()) {
                totalEarnings += p.getFareShare();
            }
            if (tvTotalEarningsShared != null) {
                tvTotalEarningsShared.setText(String.format("Total: Rs. %.0f", totalEarnings));
            }

            // Show "Find More" button if not full
            if (btnFindMorePassengers != null) {
                btnFindMorePassengers.setVisibility(
                        currentRide.canAcceptMorePassengers() ? View.VISIBLE : View.GONE);
            }
        }
    }
    private void findMorePassengers() {
        if (currentRide == null || !currentRide.canAcceptMorePassengers()) return;

        Toast.makeText(this, "Searching for more passengers...", Toast.LENGTH_SHORT).show();

        // Query for pending shared ride requests
        DatabaseReference pendingRef = FirebaseDatabase.getInstance()
                .getReference().child("customerRequest");

        pendingRef.orderByChild("status").equalTo("pending")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<SharedPassenger> potentials = new ArrayList<>();

                        for (DataSnapshot data : snapshot.getChildren()) {
                            RideRequest request = data.getValue(RideRequest.class);
                            if (request != null && request.isSharingEnabled() &&
                                    request.getVehicleType().equals(currentDriver.getVehicleType())) {

                                SharedPassenger passenger = new SharedPassenger(
                                        request.getCustomerId(),
                                        request.getCustomerName(),
                                        request.getCustomerPhone(),
                                        request.getCustomerImageUrl(),
                                        true
                                );
                                passenger.setPickupLat(request.getPickupLat());
                                passenger.setPickupLng(request.getPickupLng());
                                passenger.setPickupAddress(request.getPickupAddress());
                                passenger.setDropoffLat(request.getDestLat());
                                passenger.setDropoffLng(request.getDestLng());
                                passenger.setDropoffAddress(request.getDestinationAddress());

                                potentials.add(passenger);
                            }
                        }

                        showPotentialPassengersDialog(potentials);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void showPotentialPassengersDialog(List<SharedPassenger> passengers) {
        if (passengers.isEmpty()) {
            Toast.makeText(this, "No nearby passengers found", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[passengers.size()];
        for (int i = 0; i < passengers.size(); i++) {
            names[i] = passengers.get(i).getName() + " - " +
                    passengers.get(i).getPickupAddress();
        }

        new AlertDialog.Builder(this)
                .setTitle("Add Passenger")
                .setItems(names, (dialog, which) -> {
                    SharedPassenger selected = passengers.get(which);
                    addPassengerToRide(selected);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addPassengerToRide(SharedPassenger passenger) {
        if (!SharedRideManager.canAddPassenger(currentRide, passenger,
                new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()))) {
            Toast.makeText(this, "Passenger is not along your route", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add to ride
        SharedRideManager.updateSharedRide(currentRide, passenger);

        // Update in Firebase
        DatabaseReference rideRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("ride_info")
                .child(currentRide.getRideId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("passengers", currentRide.getPassengers());
        updates.put("currentPassengers", currentRide.getCurrentPassengers());
        rideRef.updateChildren(updates);

        // Recalculate route
        calculateRouteToPickup();

        // Update UI
        showPassengerList();
        Toast.makeText(this, passenger.getName() + " added to ride", Toast.LENGTH_SHORT).show();
    }
    private void setupPassengerList() {
        if (passengerRecyclerView != null) {
            passengerRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

        if (btnFindMorePassengers != null) {
            btnFindMorePassengers.setOnClickListener(v -> findMorePassengers());
        }
    }
    private void setupDrawerMenu() {

        navigationView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.history) {

                startActivity(new Intent(this, HistoryActivity.class)
                        .putExtra("userType", "Drivers"));

            } else if (id == R.id.earnings) {

                startActivity(new Intent(this, PayoutActivity.class));

            } else if (id == R.id.settings) {

                startActivity(new Intent(this, DriverSettingsActivity.class));

            } else if (id == R.id.help) {

                Toast.makeText(this, "Help coming soon", Toast.LENGTH_SHORT).show();

            } else if (id == R.id.logout) {

                showLogoutDialog();
            }

            drawer.closeDrawer(GravityCompat.START);
            return true;
        });
    }


    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("No", null)
                .show();
    }

    private void logout() {
        Log.d(TAG, "Performing logout");
        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();

        goOffline();
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(DriverMapActivity.this, LauncherActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupFirebase() {
        Log.d(TAG, "setupFirebase started");

        try {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                startActivity(new Intent(this, LauncherActivity.class));
                finish();
                return;
            }

            String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            driverRef = FirebaseDatabase.getInstance()
                    .getReference()
                    .child("Users")
                    .child("Drivers")
                    .child(driverId);

            rideInfoRef = FirebaseDatabase.getInstance().getReference().child("ride_info");
            geoFireWorking = new GeoFire(FirebaseDatabase.getInstance().getReference("driversWorking"));

            currentDriver = new User();
            currentDriver.setId(driverId);
            currentDriver.setUserType("driver");

            routeHelper = new RouteHelper(this, getString(R.string.google_maps_key));
            routeHelper.setCallback(this);
            notificationHelper = NotificationHelper.getInstance(this);

            Log.d(TAG, "setupFirebase completed");
        } catch (Exception e) {
            Log.e(TAG, "Error in setupFirebase", e);
        }
    }

    private void setupLocation() {
        Log.d(TAG, "setupLocation started");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        Log.d(TAG, "setupLocation completed");
    }

    private void setupMap() {
        Log.d(TAG, "setupMap started");

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
            Log.d(TAG, "MapFragment found, getMapAsync called");
        } else {
            Log.e(TAG, "MapFragment is null");
        }
    }

    private void setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView started");

        if (requestsRecyclerView == null) {
            Log.e(TAG, "requestsRecyclerView is null, cannot setup");
            return;
        }

        requestAdapter = new CardRequestAdapter(requestList, new CardRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(RideRequest request, int position) {
                acceptRide(request);
            }

            @Override
            public void onDecline(RideRequest request, int position) {
                requestList.remove(position);
                requestAdapter.notifyItemRemoved(position);
                updateRequestsVisibility();
            }
        });

        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        requestsRecyclerView.setAdapter(requestAdapter);

        Log.d(TAG, "setupRecyclerView completed");
    }

    private void updateRequestsVisibility() {
        if (requestList.isEmpty()) {
            if (noRequestsText != null) noRequestsText.setVisibility(View.VISIBLE);
            if (requestsRecyclerView != null) requestsRecyclerView.setVisibility(View.GONE);
        } else {
            if (noRequestsText != null) noRequestsText.setVisibility(View.GONE);
            if (requestsRecyclerView != null) requestsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setupBottomSheet() {
        Log.d(TAG, "setupBottomSheet started");
        setupPassengerList();
        if (bottomSheet == null) {
            Log.e(TAG, "bottomSheet is null, cannot setup");
            return;
        }

        bottomSheetView = findViewById(R.id.bottomSheet);

        try {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
            bottomSheetBehavior.setHideable(true);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        } catch (Exception e) {
            Log.e(TAG, "Error creating BottomSheetBehavior", e);
        }

        if (bringUpBottomLayout != null) {
            bringUpBottomLayout.setOnClickListener(v -> {
                if (bottomSheetBehavior != null) {
                    if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    } else {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }
                }
            });
        }

        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (currentRide == null) {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
            });
        }

        Log.d(TAG, "setupBottomSheet completed");
    }

    private void setupListeners() {
        Log.d(TAG, "setupListeners started");

        if (workingSwitch != null) {
            workingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    goOnline();
                } else {
                    goOffline();
                }
            });
        }

        if (rideStatusButton != null) {
            rideStatusButton.setOnClickListener(v -> {
                if (currentRide == null) return;

                switch (currentRide.getStatus()) {
                    case "accepted":
                        currentRide.setStatus("arrived");
                        updateRideStatus("arrived");
                        rideStatusButton.setText(R.string.start_ride);
                        break;
                    case "arrived":
                        currentRide.setStatus("started");
                        updateRideStatus("started");
                        rideStatusButton.setText(R.string.complete_ride);
                        break;
                    case "started":
                        completeRide();
                        break;
                }
            });
        }

        if (fabMaps != null) fabMaps.setOnClickListener(v -> openMaps());
        if (fabCall != null) fabCall.setOnClickListener(v -> callCustomer());
        if (cancelButton != null) cancelButton.setOnClickListener(v -> showCancelDialog());

        Log.d(TAG, "setupListeners completed");
    }

    private void goOnline() {
        if (currentDriver == null || currentDriver.getVehicleType() == null) {
            Toast.makeText(this, "Please select vehicle type first", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, DriverChooseTypeActivity.class));
            if (workingSwitch != null) workingSwitch.setChecked(false);
            return;
        }

        if (!checkLocationPermission()) {
            if (workingSwitch != null) workingSwitch.setChecked(false);
            return;
        }

        if (workingSwitch != null) workingSwitch.setChecked(true);

        // ✅ ADD THIS: Save online status to Firebase
        if (driverRef != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("isOnline", true);
            updates.put("lastOnline", System.currentTimeMillis());
            driverRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ isOnline = true saved to Firebase"))
                    .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to save online status: " + e.getMessage()));
        }

        startLocationUpdates();

        if (mMap != null && checkLocationPermission()) {
            mMap.setMyLocationEnabled(true);
        }

        startPeriodicSearch();

        if (driverStatusHeader != null) {
            driverStatusHeader.setText(R.string.online);
        }

        if (drawer != null) {
            Snackbar.make(drawer, R.string.you_are_online, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void goOffline() {
        if (workingSwitch != null) workingSwitch.setChecked(false);

        // ✅ ADD THIS: Save offline status to Firebase
        if (driverRef != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("isOnline", false);
            updates.put("lastOffline", System.currentTimeMillis());
            driverRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ isOnline = false saved to Firebase"))
                    .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to save offline status: " + e.getMessage()));
        }

        stopPeriodicSearch();
        stopLocationUpdates();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (geoFireWorking != null) {
            geoFireWorking.removeLocation(userId);
        }

        if (driverStatusHeader != null) {
            driverStatusHeader.setText(R.string.offline);
        }
        if (drawer != null) {
            Snackbar.make(drawer, R.string.you_are_offline, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (fusedLocationClient != null && locationRequest != null) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) return;

            for (Location location : locationResult.getLocations()) {
                lastLocation = location;

                // THIS IS THE CRITICAL MISSING PART - SAVE TO FIREBASE
                if (workingSwitch != null && workingSwitch.isChecked()) {
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    // Make sure geoFireWorking is initialized!
                    if (geoFireWorking != null) {
                        geoFireWorking.setLocation(userId,
                                new GeoLocation(location.getLatitude(), location.getLongitude()),
                                new GeoFire.CompletionListener() {
                                    @Override
                                    public void onComplete(String key, DatabaseError error) {
                                        if (error != null) {
                                            Log.e(TAG, "GeoFire error: " + error.getMessage());
                                        } else {
                                            Log.d(TAG, "Driver location saved to Firebase");
                                        }
                                    }
                                });
                    } else {
                        Log.e(TAG, "geoFireWorking is null! Reinitializing...");
                        geoFireWorking = new GeoFire(FirebaseDatabase.getInstance()
                                .getReference("driversWorking"));
                        // Try again
                        geoFireWorking.setLocation(userId,
                                new GeoLocation(location.getLatitude(), location.getLongitude()));
                    }

                    // Update last updated timestamp
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("last_updated", ServerValue.TIMESTAMP);
                    if (driverRef != null) driverRef.updateChildren(updates);
                }

                // Update map camera if needed
                if (!zoomUpdated && mMap != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(location.getLatitude(), location.getLongitude()), 16));
                    zoomUpdated = true;
                }


                if (!started && workingSwitch != null && workingSwitch.isChecked()) {
                   searchForRequests();
                    started = true;
                }

                // Update ride distance if ride is in progress
                if (currentRide != null && "started".equals(currentRide.getStatus())) {
                    updateRideDistance(location);
                }
            }
        }
    };

    private void searchForRequests() {
        if (lastLocation == null) {
            Log.e(TAG, "❌ Cannot search: lastLocation is null");
            return;
        }

        Log.d(TAG, "======================================");
        Log.d(TAG, "🔍 DRIVER SEARCHING FOR REQUESTS");
        Log.d(TAG, "📍 Driver location: " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude());
        Log.d(TAG, "🚗 Driver vehicle: " + (currentDriver != null ? currentDriver.getVehicleType() : "null"));
        Log.d(TAG, "📏 Search radius: " + MAX_SEARCH_DISTANCE + " km");
        Log.d(TAG, "======================================");

        DatabaseReference requestsLocation = FirebaseDatabase.getInstance()
                .getReference().child("customerRequest");

        GeoFire geoFire = new GeoFire(requestsLocation);
        geoQuery = geoFire.queryAtLocation(
                new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()),
                MAX_SEARCH_DISTANCE);

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                Log.d(TAG, "📢 Found request nearby: " + key);
                Log.d(TAG, "   Location: " + location.latitude + ", " + location.longitude);

                if (workingSwitch == null || !workingSwitch.isChecked() || currentRide != null) {
                    Log.d(TAG, "   Skipping: workingSwitch=" + (workingSwitch != null && workingSwitch.isChecked()) +
                            ", currentRide=" + (currentRide != null));
                    return;
                }

                // Check if request already in list
                for (RideRequest ride : requestList) {
                    if (ride.getRideId() != null && ride.getRideId().equals(key)) {
                        Log.d(TAG, "   Already in list");
                        return;
                    }
                }

                fetchRequestDetails(key);
            }

            @Override
            public void onKeyExited(String key) {}

            @Override
            public void onKeyMoved(String key, GeoLocation location) {}

            @Override
            public void onGeoQueryReady() {
                Log.d(TAG, "✅ GeoQuery complete - no more requests");
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e(TAG, "❌ GeoQuery error: " + error.getMessage());
            }
        });
    }

    private void fetchRequestDetails(String requestId) {
        Log.d(TAG, "🔍 Fetching details for request: " + requestId);

        DatabaseReference customerRequestRef = FirebaseDatabase.getInstance()
                .getReference().child("customerRequest").child(requestId);

        customerRequestRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.d(TAG, "   Request doesn't exist in customerRequest");
                    return;
                }
                if (currentRide != null) {
                    Log.d(TAG, "   Already have a ride, skipping");
                    return;
                }

                // Manually parse the data instead of using getValue()
                String vehicleType = snapshot.child("vehicleType").getValue(String.class);
                String status = snapshot.child("status").getValue(String.class);
                Double pickupLat = snapshot.child("pickupLat").getValue(Double.class);
                Double pickupLng = snapshot.child("pickupLng").getValue(Double.class);
                Double destLat = snapshot.child("destLat").getValue(Double.class);
                Double destLng = snapshot.child("destLng").getValue(Double.class);
                String pickupAddress = snapshot.child("pickupAddress").getValue(String.class);
                String destinationAddress = snapshot.child("destinationAddress").getValue(String.class);
                String customerId = snapshot.child("customerId").getValue(String.class);
                String customerName = snapshot.child("customerName").getValue(String.class);
                String customerPhone = snapshot.child("customerPhone").getValue(String.class);
                String customerImageUrl = snapshot.child("customerImageUrl").getValue(String.class);
                Double fare = snapshot.child("fare").getValue(Double.class);

                // Create a new RideRequest object and manually set values
                RideRequest request = new RideRequest();
                request.setRideId(requestId);
                request.setVehicleType(vehicleType);
                request.setStatus(status);
                request.setPickupLat(pickupLat != null ? pickupLat : 0);
                request.setPickupLng(pickupLng != null ? pickupLng : 0);
                request.setDestLat(destLat != null ? destLat : 0);
                request.setDestLng(destLng != null ? destLng : 0);
                request.setPickupAddress(pickupAddress);
                request.setDestinationAddress(destinationAddress);
                request.setCustomerId(customerId);
                request.setCustomerName(customerName);
                request.setCustomerPhone(customerPhone);
                request.setCustomerImageUrl(customerImageUrl);
                request.setFare(fare != null ? fare : 0);
                request.setSharingEnabled(snapshot.child("sharingEnabled").getValue(Boolean.class) != null ?
                        snapshot.child("sharingEnabled").getValue(Boolean.class) : false);

                Log.d(TAG, "   Request details:");
                Log.d(TAG, "      Vehicle type: " + vehicleType);
                Log.d(TAG, "      Status: " + status);
                Log.d(TAG, "      Pickup: " + (pickupLat != null ? pickupLat : 0) + ", " + (pickupLng != null ? pickupLng : 0));

                // Check if vehicle type matches
                if (currentDriver == null) {
                    Log.d(TAG, "   currentDriver is null");
                    return;
                }

                if (vehicleType == null || !vehicleType.equals(currentDriver.getVehicleType())) {
                    Log.d(TAG, "   ❌ Vehicle mismatch: " + vehicleType + " vs " + currentDriver.getVehicleType());
                    return;
                }

                // Check if request is still pending
                if (status == null || !"pending".equals(status)) {
                    Log.d(TAG, "   ❌ Request not pending: " + status);
                    return;
                }

                Log.d(TAG, "   ✅ MATCH FOUND! Adding to list");
                requestList.add(request);
                if (requestAdapter != null) {
                    requestAdapter.notifyItemInserted(requestList.size() - 1);
                }

                updateRequestsVisibility();
                playNotificationSound();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching request details: " + error.getMessage());
            }
        });
    }

    private void playNotificationSound() {
        try {
            MediaPlayer.create(this, R.raw.driver_notification).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void acceptRide(RideRequest request) {
        stopPeriodicSearch();
        currentRide = request;
        if (currentDriver != null) {
            currentRide.setDriverId(currentDriver.getId());
            currentRide.setDriverName(currentDriver.getName());
            currentRide.setDriverPhone(currentDriver.getPhone());
        }
        currentRide.setStatus("accepted");
        currentRide.setAcceptedAt(System.currentTimeMillis());

        // Update in Firebase
        Map<String, Object> updates = new HashMap<>();
        if (currentDriver != null) {
            updates.put("driverId", currentDriver.getId());
            updates.put("driverName", currentDriver.getName());
            updates.put("driverPhone", currentDriver.getPhone());
        }
        updates.put("status", "accepted");
        updates.put("acceptedAt", System.currentTimeMillis());

        if (rideInfoRef != null && request.getRideId() != null) {
            rideInfoRef.child(request.getRideId()).updateChildren(updates);
        }

        // Update driver's current ride
        if (driverRef != null && request.getRideId() != null) {
            driverRef.child("currentRideId").setValue(request.getRideId());
        }

        // Clear request list
        requestList.clear();
        if (requestAdapter != null) {
            requestAdapter.notifyDataSetChanged();
        }
        updateRequestsVisibility();

        // Show customer info
        showCustomerInfo();

        // Start listening for ride updates
        listenForRideUpdates();

        // Calculate route to pickup
        calculateRouteToPickup();
    }

    private void showCustomerInfo() {
        if (currentRide == null) return;

        if (customerInfo != null) customerInfo.setVisibility(View.VISIBLE);
        if (customerName != null) customerName.setText(currentRide.getCustomerName());
        if (pickupAddress != null) pickupAddress.setText(currentRide.getPickupAddress());


        // Load customer image
        if (customerProfileImage != null &&
                currentRide.getCustomerImageUrl() != null &&
                !currentRide.getCustomerImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentRide.getCustomerImageUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(customerProfileImage);
        }

        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setHideable(false);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }

        // Show passenger list for shared rides
        showPassengerList();
    }

    private void listenForRideUpdates() {
        if (currentRide == null || rideInfoRef == null || currentRide.getRideId() == null) return;

        rideStatusListener = rideInfoRef.child(currentRide.getRideId())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;

                        String status = snapshot.child("status").getValue(String.class);
                        if (status == null) return;

                        if (currentRide != null) {
                            currentRide.setStatus(status);
                        }

                        switch (status) {
                            case "cancelled":
                            case "completed":
                                endRide();
                                break;
                            case "started":
                                if (rideStatusButton != null) {
                                    rideStatusButton.setText(R.string.complete_ride);
                                }
                                break;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void startPeriodicSearch() {
        if (isSearching) return;

        isSearching = true;
        searchRunnable = new Runnable() {
            @Override
            public void run() {
                if (workingSwitch != null && workingSwitch.isChecked() && currentRide == null) {
                    Log.d(TAG, "🔍 Periodic search for requests...");
                    searchForRequests();
                }
                // Repeat every 10 seconds
                if (isSearching) {
                    searchHandler.postDelayed(this, 10000);
                }
            }
        };

        // Start immediately
        searchHandler.post(searchRunnable);
        Log.d(TAG, "✅ Started periodic search (every 10 seconds)");
    }

    private void stopPeriodicSearch() {
        isSearching = false;
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        Log.d(TAG, "Stopped periodic search");
    }

    private void calculateRouteToPickup() {
        if (lastLocation == null || currentRide == null || routeHelper == null) return;

        LatLng origin = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        LatLng destination = new LatLng(
                currentRide.getPickupLat(),
                currentRide.getPickupLng());

        routeHelper.getRoute(origin, destination);
    }

    private void calculateRouteToDestination() {
        if (lastLocation == null || currentRide == null || routeHelper == null) return;

        LatLng origin = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        LatLng destination = new LatLng(
                currentRide.getDestLat(),
                currentRide.getDestLng());

        routeHelper.getRoute(origin, destination);
    }

    private void updateRideStatus(String status) {
        if (currentRide == null || rideInfoRef == null || currentRide.getRideId() == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);

        switch (status) {
            case "arrived":
                updates.put("arrivedTime", System.currentTimeMillis());
                break;
            case "started":
                updates.put("startedTime", System.currentTimeMillis());
                calculateRouteToDestination();
                break;
        }

        rideInfoRef.child(currentRide.getRideId()).updateChildren(updates);
    }

    private void updateRideDistance(Location newLocation) {
        if (lastLocation == null || currentRide == null || rideInfoRef == null ||
                currentRide.getRideId() == null) return;

        float distanceDelta = lastLocation.distanceTo(newLocation) / 1000; // in km
        double totalDistance = currentRide.getDistance() + distanceDelta;

        Map<String, Object> updates = new HashMap<>();
        updates.put("distance", totalDistance);

        rideInfoRef.child(currentRide.getRideId()).updateChildren(updates);
    }

    private void completeRide() {
        if (currentRide == null || rideInfoRef == null || currentRide.getRideId() == null) return;

        double fare = calculateFare(currentRide.getDistance());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "completed");
        updates.put("completedTime", System.currentTimeMillis());
        updates.put("fare", fare);

        rideInfoRef.child(currentRide.getRideId()).updateChildren(updates);

        // Update driver earnings
        updateDriverEarnings(fare);
    }

    private double calculateFare(double distanceKm) {
        double baseFare = 50;
        double perKmRate = 15;
        double minimumFare = 100;

        double fare = baseFare + (distanceKm * perKmRate);
        return Math.max(fare, minimumFare);
    }

    private void updateDriverEarnings(double fare) {
        if (driverRef == null) return;

        driverRef.child("earnings").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double totalEarnings = snapshot.child("total").exists() ?
                        snapshot.child("total").getValue(Double.class) : 0;
                double availableEarnings = snapshot.child("available").exists() ?
                        snapshot.child("available").getValue(Double.class) : 0;

                Map<String, Object> earnings = new HashMap<>();
                earnings.put("total", totalEarnings + fare);
                earnings.put("available", availableEarnings + fare);

                driverRef.child("earnings").setValue(earnings);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showCancelDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.cancel_ride)
                .setMessage(R.string.cancel_ride_confirmation)
                .setPositiveButton(R.string.yes, (dialog, which) -> cancelRide())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void cancelRide() {
        if (currentRide == null || rideInfoRef == null || currentRide.getRideId() == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "cancelled");
        updates.put("cancelledTime", System.currentTimeMillis());
        updates.put("cancelledBy", "driver");

        rideInfoRef.child(currentRide.getRideId()).updateChildren(updates);
        endRide();
    }

    private void endRide() {
        if (rideStatusListener != null && currentRide != null && rideInfoRef != null &&
                currentRide.getRideId() != null) {
            rideInfoRef.child(currentRide.getRideId()).removeEventListener(rideStatusListener);
        }

        // Clear driver's current ride
        if (driverRef != null) {
            driverRef.child("currentRideId").removeValue();
        }

        // Remove markers
        if (pickupMarker != null) pickupMarker.remove();
        if (destinationMarker != null) destinationMarker.remove();

        // Clear polylines
        for (Polyline polyline : polylines) {
            polyline.remove();
        }
        polylines.clear();

        // Reset UI
        currentRide = null;
        if (customerInfo != null) customerInfo.setVisibility(View.GONE);
        if (passengerListLayout != null) passengerListLayout.setVisibility(View.GONE);
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setHideable(true);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
        if (rideStatusButton != null) {
            rideStatusButton.setText(R.string.pickup_customer);
        }

        // Clear map and restart search
        if (mMap != null) mMap.clear();
        zoomUpdated = false;
        started = false;

        if (workingSwitch != null && workingSwitch.isChecked()) {
            searchForRequests();
        }
    }

    private void openMaps() {
        if (currentRide == null) return;

        double lat, lng;
        if ("accepted".equals(currentRide.getStatus())) {
            lat = currentRide.getPickupLat();
            lng = currentRide.getPickupLng();
        } else {
            lat = currentRide.getDestLat();
            lng = currentRide.getDestLng();
        }

        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Uri webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng);
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }

    private void callCustomer() {
        if (currentRide == null || currentRide.getCustomerPhone() == null || currentRide.getCustomerPhone().isEmpty()) {
            Toast.makeText(this,"Customer phone not available",Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_CALL,
                    Uri.parse("tel:" + currentRide.getCustomerPhone()));
            startActivity(intent);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void loadDriverData() {
        if (driverRef == null) return;

        driverRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                if (currentDriver != null) {
                    currentDriver.setName(snapshot.child("name").getValue(String.class));
                    currentDriver.setPhone(snapshot.child("phone").getValue(String.class));
                    currentDriver.setCar(snapshot.child("car").getValue(String.class));
                    currentDriver.setVehicleType(snapshot.child("vehicleType").getValue(String.class));

                    // ✅ ADD THIS: Sync switch with Firebase
                    Boolean isOnline = snapshot.child("isOnline").getValue(Boolean.class);
                    if (isOnline != null && workingSwitch != null) {
                        workingSwitch.setChecked(isOnline);
                    }

                    // Force driver to choose vehicle type if not set
                    if (currentDriver.getVehicleType() == null) {
                        startActivity(new Intent(DriverMapActivity.this, DriverChooseTypeActivity.class));
                    }

                    Boolean active = snapshot.child("active").getValue(Boolean.class);
                    if (active != null && !active) {
                        if (workingSwitch != null) {
                            workingSwitch.setChecked(false);
                            workingSwitch.setEnabled(false);
                        }
                        Toast.makeText(DriverMapActivity.this, R.string.not_approved, Toast.LENGTH_LONG).show();
                    }

                    if (driverNameHeader != null) {
                        driverNameHeader.setText(currentDriver.getName());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkForActiveRide() {
        if (driverRef == null) return;

        driverRef.child("currentRideId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String rideId = snapshot.getValue(String.class);
                if (rideId == null || rideId.isEmpty() || rideInfoRef == null) return;

                rideInfoRef.child(rideId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) return;

                        currentRide = dataSnapshot.getValue(RideRequest.class);
                        if (currentRide == null) return;

                        currentRide.setRideId(rideId);

                        String status = currentRide.getStatus();
                        if ("accepted".equals(status) || "started".equals(status)) {
                            showCustomerInfo();
                            listenForRideUpdates();

                            if ("accepted".equals(status)) {
                                calculateRouteToPickup();
                            } else {
                                calculateRouteToDestination();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (workingSwitch != null && workingSwitch.isChecked()) {
                    startLocationUpdates();
                }
            } else {
                Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_LONG).show();
                if (workingSwitch != null) workingSwitch.setChecked(false);
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "Map is ready");

        if (checkLocationPermission()) {
            if (checkLocationPermission()) {
                mMap.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    public void onRouteSuccess(ArrayList<LatLng> path, double distance, int duration) {
        if (mMap == null) return;

        // Clear old polylines
        for (Polyline polyline : polylines) {
            polyline.remove();
        }
        polylines.clear();

        // Draw new route
        PolylineOptions options = new PolylineOptions()
                .addAll(path)
                .width(12)
                .color(ContextCompat.getColor(this, R.color.colorPrimary))
                .geodesic(true);

        polylines.add(mMap.addPolyline(options));

        // Add markers if needed
        if (currentRide != null) {
            if (pickupMarker == null && currentRide.getPickupLat() != 0) {
                pickupMarker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(currentRide.getPickupLat(), currentRide.getPickupLng()))
                        .title("Pickup")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            }

            if (destinationMarker == null && currentRide.getDestLat() != 0) {
                destinationMarker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(currentRide.getDestLat(), currentRide.getDestLng()))
                        .title("Destination")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            }
        }

        // Zoom to show entire route
        if (!path.isEmpty()) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng point : path) {
                builder.include(point);
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
        }
    }

    @Override
    public void onRouteFailure(String error) {
        Log.e(TAG, "Route failure: " + error);
        if (drawer != null) {
            Snackbar.make(drawer, "Route error: " + error, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        stopPeriodicSearch();

        if (rideStatusListener != null && currentRide != null && rideInfoRef != null &&
                currentRide.getRideId() != null) {
            rideInfoRef.child(currentRide.getRideId()).removeEventListener(rideStatusListener);
        }

        if (geoQuery != null) {
            geoQuery.removeAllListeners();
        }

        Log.d(TAG, "onDestroy completed");
    }
}