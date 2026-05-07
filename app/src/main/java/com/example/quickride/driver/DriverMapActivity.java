package com.example.quickride.driver;

import android.os.Build;

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
import androidx.annotation.Nullable;
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
    private PassengerAdapter passengerAdapter;
    private static final String TAG = "DriverMapActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final int GPS_SETTINGS_REQUEST_CODE = 1001;
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
    private TextView customerName, pickupAddress, driverNameHeader, driverStatusHeader, pullUpCustomerName;
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
    private ValueEventListener requestsEventListener;
    private GeoQuery geoQuery;


    // Map
    private Marker pickupMarker, destinationMarker;
    private List<Polyline> polylines = new ArrayList<>();
    private boolean zoomUpdated = false;
    private boolean started = false;

    // Helpers
    private RouteHelper routeHelper;
    private NotificationHelper notificationHelper;
    private com.example.quickride.utils.VoiceHelper voiceHelper;
    private int lastRequestCount = 0;
    private com.example.quickride.utils.LocationHelper locationHelper;


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
        
        // Add safety check: if user is not logged in or session expired, prevent startup crashes
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Session expired or invalid login. Redirecting...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LauncherActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        
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
        pullUpCustomerName = findViewById(R.id.pullUpCustomerName);
        
        // --- ADDED MISSING INITIALIZATIONS ---
        customerName = findViewById(R.id.customerName);
        pickupAddress = findViewById(R.id.pickupAddress);
        customerProfileImage = findViewById(R.id.customerProfileImage);
        // -------------------------------------

        passengerRecyclerView = findViewById(R.id.passengerRecyclerView);
        tvPassengerCountHeader = findViewById(R.id.tvPassengerCountHeader);
        tvTotalEarningsShared = findViewById(R.id.tvTotalEarningsShared);
        passengerListLayout = findViewById(R.id.passengerListLayout);
        btnFindMorePassengers = findViewById(R.id.btnFindMorePassengers);
    }

    private void setupToolbar() {
        if (drawerButton != null) {
            drawerButton.setOnClickListener(v -> drawer.openDrawer(GravityCompat.START));
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
            } else if (id == R.id.fixed_route) {
                startActivity(new Intent(this, CreateFixedRouteActivity.class));
            } else if (id == R.id.view_bookings) {
                startActivity(new Intent(this, ViewFixedRouteBookingsActivity.class));
            } else if (id == R.id.settings) {
                startActivity(new Intent(this, DriverSettingsActivity.class));
            } else if (id == R.id.help) {
                Toast.makeText(this, "Help coming soon", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.logout) {
                logout();
            }

            drawer.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setupFirebase() {
        Log.d(TAG, "setupFirebase started");

        try {
            String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            driverRef = FirebaseDatabase.getInstance().getReference("Users").child("Drivers").child(driverId);
            rideInfoRef = FirebaseDatabase.getInstance().getReference("ride_info");
            geoFireWorking = new GeoFire(FirebaseDatabase.getInstance().getReference("driversWorking"));

            currentDriver = new User();
            listenForFixedRouteBookings(driverId);
            currentDriver.setId(driverId);
            currentDriver.setUserType("driver");

            routeHelper = new RouteHelper(this, getString(R.string.google_maps_key));
            routeHelper.setCallback(this);
            notificationHelper = NotificationHelper.getInstance(this);
            notificationHelper.initialize("Drivers", driverId);
            voiceHelper = new com.example.quickride.utils.VoiceHelper(this);
            locationHelper = new com.example.quickride.utils.LocationHelper(this);



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
        
        if (locationHelper != null) {
            locationHelper.checkLocationSettings(this, GPS_SETTINGS_REQUEST_CODE);
        }

        if (checkLocationPermission()) {
            try {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        lastLocation = location;
                        if (mMap != null && !zoomUpdated) {
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                            zoomUpdated = true;
                        }
                    }
                });
                // Start getting background updates immediately to warm up GPS
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            } catch (SecurityException e) {
                Log.e(TAG, "Missing location permission", e);
            }
        }

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
                acceptRideRequest(request);
            }

            @Override
            public void onDecline(RideRequest request, int position) {
                declineRideRequest(request, position);
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
            
            // Only set "No Active Ride" if there is no current ride
            if (currentRide == null && pullUpCustomerName != null) {
                pullUpCustomerName.setText("No Active Ride");
            }

            // Keep bottom sheet visible but collapsed if no requests and no ride
            if (currentRide == null && bottomSheetBehavior != null) {
                bottomSheetBehavior.setHideable(false);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        } else {
            if (noRequestsText != null) noRequestsText.setVisibility(View.GONE);
            if (requestsRecyclerView != null) requestsRecyclerView.setVisibility(View.VISIBLE);
            
            // If in a ride, maybe keep the ride name or show both? 
            // For now, prioritize request count on the handle if not in a ride
            if (currentRide == null && pullUpCustomerName != null) {
                pullUpCustomerName.setText(requestList.size() + " New Request" + (requestList.size() > 1 ? "s" : ""));
            }

            // Ensure the bottom sheet is visible
            if (bottomSheetView != null) {
                bottomSheetView.setVisibility(View.VISIBLE);
            }
            if (bottomSheetBehavior != null) {
                bottomSheetBehavior.setHideable(false); // Prevent accidental hiding
                
                // Refresh peek height in case handle layout is now measured
                if (bringUpBottomLayout != null && bringUpBottomLayout.getHeight() > 0) {
                    bottomSheetBehavior.setPeekHeight(bringUpBottomLayout.getHeight());
                } else {
                    bottomSheetBehavior.setPeekHeight(200);
                }
                
                // Only change state if it was hidden
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        }
    }

    private void setupBottomSheet() {
        Log.d(TAG, "setupBottomSheet started");
        
        bottomSheetView = findViewById(R.id.bottomSheet);
        if (bottomSheetView == null) {
            Log.e(TAG, "bottomSheetView is null, cannot setup");
            return;
        }

        try {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
            bottomSheetBehavior.setHideable(true);
            
            // Set initial peek height to the height of the handle layout
            if (bringUpBottomLayout != null) {
                bringUpBottomLayout.post(() -> {
                    int handleHeight = bringUpBottomLayout.getHeight();
                    if (handleHeight > 0) {
                        bottomSheetBehavior.setPeekHeight(handleHeight);
                    } else {
                        bottomSheetBehavior.setPeekHeight(200);
                    }
                });
            }

            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            
            bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN && (!requestList.isEmpty() || currentRide != null)) {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                }
            });
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

        if (fusedLocationClient != null) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }

        if (driverStatusHeader != null) {
            driverStatusHeader.setText("Online");
            driverStatusHeader.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        }

        // Start Foreground Service for background persistence
        Intent serviceIntent = new Intent(this, com.example.quickride.services.DriverForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        startSearchingForRequests();

    }

    private void goOffline() {
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        geoFireWorking.removeLocation(userId);

        if (driverStatusHeader != null) {
            driverStatusHeader.setText("Offline");
            driverStatusHeader.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        }

        // Stop Foreground Service
        Intent serviceIntent = new Intent(this, com.example.quickride.services.DriverForegroundService.class);
        stopService(serviceIntent);

        stopSearchingForRequests();

    }

    private void startSearchingForRequests() {
        if (isSearching) return;
        isSearching = true;

        Log.d(TAG, "Starting to search for requests");
        
        DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference("customerRequest");
        requestsEventListener = requestsRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isSearching) return;
                Log.d(TAG, "onDataChange: Received " + snapshot.getChildrenCount() + " raw requests from Firebase");
                
                requestList.clear();
                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    RideRequest request = requestSnapshot.getValue(RideRequest.class);
                    if (request != null && "pending".equals(request.getStatus())) {
                        String rId = requestSnapshot.getKey();
                        if (rId != null) {
                            request.setRideId(rId);
                            if (request.getCustomerId() == null) request.setCustomerId(rId);
                        }
                        if (lastLocation != null) {
                            String currentDriverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            String trustedOnly = requestSnapshot.child("trustedDriverOnly").getValue(String.class);
                            if (trustedOnly != null && !trustedOnly.isEmpty() && !trustedOnly.equals(currentDriverId)) {
                                Log.d(TAG, "Request skipped: trustedDriverOnly is set to another driver");
                                continue;
                            }

                            float[] results = new float[1];
                            Location.distanceBetween(lastLocation.getLatitude(), lastLocation.getLongitude(),
                                    request.getPickupLat(), request.getPickupLng(), results);
                            float distanceInKm = results[0] / 1000;
                            
                            Log.d(TAG, "Evaluating Ride ID " + request.getRideId() + ": Distance = " + String.format(Locale.US, "%.2f", distanceInKm) + " km");
                            
                            if (distanceInKm <= MAX_SEARCH_DISTANCE) {
                                requestList.add(request);
                                Log.d(TAG, "Request accepted: within " + MAX_SEARCH_DISTANCE + " km radius");
                            } else {
                                Log.d(TAG, "Request skipped: outside " + MAX_SEARCH_DISTANCE + " km radius");
                            }
                        } else {
                            Log.w(TAG, "lastLocation is null, including request by default");
                            requestList.add(request);
                        }
                    } else if (request != null) {
                        Log.v(TAG, "Skipping ride " + request.getRideId() + " because status is " + request.getStatus());
                    } else {
                        Log.w(TAG, "Request object is null for snapshot: " + requestSnapshot.getKey());
                    }
                }

                
                Log.d(TAG, "Final filtered request list size: " + requestList.size());
                requestAdapter.notifyDataSetChanged();
                updateRequestsVisibility();

                int currentCount = requestList.size();
                if (currentCount > lastRequestCount) {
                    if (voiceHelper != null) {
                        voiceHelper.speak("New ride request received");
                    }
                }
                lastRequestCount = currentCount;

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error searching for requests", error.toException());
            }
        });
    }

    private void stopSearchingForRequests() {
        isSearching = false;
        if (requestsEventListener != null) {
            FirebaseDatabase.getInstance().getReference("customerRequest").removeEventListener(requestsEventListener);
            requestsEventListener = null;
        }
        requestList.clear();

        requestAdapter.notifyDataSetChanged();
        updateRequestsVisibility();
    }

    private void acceptRideRequest(RideRequest request) {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        String reqKey = request.getCustomerId() != null ? request.getCustomerId() : request.getRideId();
        if (reqKey == null || reqKey.trim().isEmpty()) {
            Toast.makeText(this, "Error: Invalid request data. Re-syncing...", Toast.LENGTH_SHORT).show();
            int idx = requestList.indexOf(request);
            if (idx >= 0) {
                requestList.remove(idx);
                requestAdapter.notifyDataSetChanged();
                updateRequestsVisibility();
            }
            return;
        }
        
        try {
            DatabaseReference rideRef = FirebaseDatabase.getInstance().getReference("ride_info").child(reqKey);
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("driverId", driverId);
            updates.put("status", "accepted");
            
            rideRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                try {
                    // Update customer request so Rider gets the handshake!
                    DatabaseReference customerReqRef = FirebaseDatabase.getInstance().getReference("customerRequest").child(reqKey);
                    Map<String, Object> customerUpdates = new HashMap<>();
                    customerUpdates.put("driverId", driverId);
                    customerUpdates.put("status", "accepted");
                    customerReqRef.updateChildren(customerUpdates);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to update customerRequest", e);
                }
                
                currentRide = request;
                currentRide.setStatus("accepted");
                
                showCurrentRideUI();
                calculateRouteToPickup();
            });
        } catch (Exception e) {
            Toast.makeText(this, "Corrupt Ride Data Detected.", Toast.LENGTH_SHORT).show();
            try {
                int idx = requestList.indexOf(request);
                if (idx >= 0) {
                    requestList.remove(idx);
                    requestAdapter.notifyDataSetChanged();
                    updateRequestsVisibility();
                }
            } catch (Exception ex) {}
        }
    }

    private void declineRideRequest(RideRequest request, int position) {
        String reqKey = request.getCustomerId() != null ? request.getCustomerId() : request.getRideId();
        
        try {
            if (reqKey != null && !reqKey.trim().isEmpty()) {
                FirebaseDatabase.getInstance().getReference("customerRequest")
                        .child(reqKey)
                        .child("status")
                        .setValue("declined");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to decline in Firebase due to bad key", e);
        }
        
        try {
            int idx = requestList.indexOf(request);
            if (idx >= 0) {
                requestList.remove(idx);
            }
            // Use notifyDataSetChanged to avoid "Inconsistency detected" RecyclerView crashes
            requestAdapter.notifyDataSetChanged();
            updateRequestsVisibility();
        } catch (Exception e) {
            // Silent fallback just in case
        }
    }

    private void showCurrentRideUI() {
        if (currentRide == null) return;

        customerInfo.setVisibility(View.VISIBLE);
        requestsRecyclerView.setVisibility(View.GONE);
        noRequestsText.setVisibility(View.GONE);

        // Fix "Ride: null" bug – safely fall back to a placeholder name
        String custName = currentRide.getCustomerName();
        if (custName == null || custName.trim().isEmpty()) custName = "Passenger";
        customerName.setText(custName);
        pickupAddress.setText(currentRide.getPickupAddress());

        if (pullUpCustomerName != null) {
            pullUpCustomerName.setText("Ride: " + custName);
        }

        if (currentRide.getCustomerImageUrl() != null && !currentRide.getCustomerImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentRide.getCustomerImageUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .into(customerProfileImage);
        }

        rideStatusButton.setVisibility(View.VISIBLE);
        
        // Set correct button text based on status
        if ("accepted".equals(currentRide.getStatus())) {
            rideStatusButton.setText(R.string.status_arrived);
        } else if ("arrived".equals(currentRide.getStatus())) {
            rideStatusButton.setText(R.string.start_ride);
        } else if ("started".equals(currentRide.getStatus())) {
            rideStatusButton.setText(R.string.complete_ride);
        }
        
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setHideable(false);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
        
        // Listen for Rider cancellation
        if (rideStatusListener != null) {
            FirebaseDatabase.getInstance().getReference("ride_info")
                    .child(currentRide.getRideId()).removeEventListener(rideStatusListener);
        }
        
        rideStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                String status = snapshot.child("status").getValue(String.class);
                String cancelledBy = snapshot.child("cancelledBy").getValue(String.class);
                
                if ("cancelled".equals(status) && "customer".equals(cancelledBy)) {
                    Toast.makeText(DriverMapActivity.this, "Rider has cancelled the trip.", Toast.LENGTH_LONG).show();
                    new androidx.appcompat.app.AlertDialog.Builder(DriverMapActivity.this)
                            .setTitle("Trip Cancelled")
                            .setMessage("The customer has cancelled this ride request.")
                            .setPositiveButton("OK", null)
                            .show();
                    
                    // Cleanup locally since the ride is dead
                    currentRide = null;
                    customerInfo.setVisibility(View.GONE);
                    rideStatusButton.setVisibility(View.GONE);
                    mMap.clear();
                    updateRequestsVisibility();
                    
                    if (rideStatusListener != null) {
                        snapshot.getRef().removeEventListener(rideStatusListener);
                        rideStatusListener = null;
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        
        FirebaseDatabase.getInstance().getReference("ride_info")
                .child(currentRide.getRideId()).addValueEventListener(rideStatusListener);
    }

    private void updateRideStatus(String status) {
        if (currentRide == null || currentRide.getRideId() == null) return;
        
        DatabaseReference rideRef = FirebaseDatabase.getInstance().getReference("ride_info").child(currentRide.getRideId());
        rideRef.child("status").setValue(status);
        
        // Notify Rider
        FirebaseDatabase.getInstance().getReference("customerRequest")
            .child(currentRide.getRideId()).child("status").setValue(status);
        
        if ("arrived".equals(status)) {
            calculateRouteToDestination();
        }
    }

    private void completeRide() {
        if (currentRide == null || currentRide.getRideId() == null) return;

        if (voiceHelper != null) {
            voiceHelper.speak("Ride completed successfully");
        }

        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        long completedTimestamp = System.currentTimeMillis();

        // ── Fare Calculation: Base + (KM × Rate) ────────────────────────────
        // If fare was not pre-calculated, compute it now from distance
        double fare = currentRide.getFare();
        double distanceKm = currentRide.getDistance();
        if (fare <= 0 && distanceKm > 0) {
            double baseFare = 50.0;
            double ratePerKm = 15.0;
            fare = baseFare + (distanceKm * ratePerKm);
            currentRide.setFare(fare);
        } else if (fare <= 0) {
            fare = 50.0; // Minimum fare fallback
            currentRide.setFare(fare);
        }

        // ── Build completed ride data ────────────────────────────────────────
        Map<String, Object> completedData = new HashMap<>();
        completedData.put("status", "completed");
        completedData.put("timestamp", completedTimestamp);
        completedData.put("driverId", driverId);
        completedData.put("fare", fare);
        completedData.put("distance", distanceKm);

        if (currentDriver != null) {
            if (currentDriver.getName() != null)           completedData.put("driverName", currentDriver.getName());
            if (currentDriver.getPhone() != null)          completedData.put("driverPhone", currentDriver.getPhone());
            if (currentDriver.getProfileImageUrl() != null) completedData.put("driverImageUrl", currentDriver.getProfileImageUrl());
            if (currentDriver.getVehicleType() != null)    completedData.put("car", currentDriver.getVehicleType());
        }

        if (currentRide.getCustomerId() != null)         completedData.put("customerId", currentRide.getCustomerId());
        if (currentRide.getCustomerName() != null)       completedData.put("customerName", currentRide.getCustomerName());
        if (currentRide.getCustomerPhone() != null)      completedData.put("customerPhone", currentRide.getCustomerPhone());
        if (currentRide.getCustomerImageUrl() != null)   completedData.put("customerImageUrl", currentRide.getCustomerImageUrl());
        if (currentRide.getPickupAddress() != null)      completedData.put("pickupAddress", currentRide.getPickupAddress());
        if (currentRide.getDestinationAddress() != null) completedData.put("destinationAddress", currentRide.getDestinationAddress());
        completedData.put("pickupLat", currentRide.getPickupLat());
        completedData.put("pickupLng", currentRide.getPickupLng());
        completedData.put("destLat", currentRide.getDestLat());
        completedData.put("destLng", currentRide.getDestLng());
        completedData.put("paymentMethod", currentRide.getPaymentMethod() != null ? currentRide.getPaymentMethod() : "cash");

        final String rideId = currentRide.getRideId();
        final double finalFare = fare;

        // ── Save to ride_info ────────────────────────────────────────────────
        try {
            DatabaseReference rideRef = FirebaseDatabase.getInstance().getReference("ride_info").child(rideId);
            rideRef.updateChildren(completedData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ ride_info updated"))
                    .addOnFailureListener(e -> Log.e(TAG, "❌ ride_info update failed", e));
        } catch (Exception e) {
            Log.e(TAG, "Exception writing ride_info", e);
        }

        // ── Also save to dedicated History node with UNIQUE ID ───────────────
        try {
            DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("History").push();
            historyRef.setValue(completedData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ History node written with unique ID"))
                    .addOnFailureListener(e -> Log.e(TAG, "❌ History write failed", e));
        } catch (Exception e) {
            Log.e(TAG, "Exception writing History node", e);
        }

        // ── Update Driver Earnings and Stats ──────────────────────────────────
        updateDriverEarnings(driverId, finalFare, distanceKm);

        // ── Notify customerRequest so Rider app updates ──────────────────────
        try {
            FirebaseDatabase.getInstance().getReference("customerRequest")
                    .child(rideId).child("status").setValue("completed");
        } catch (Exception e) {
            Log.e(TAG, "Exception notifying customerRequest", e);
        }

        // ── Clean up local UI state before showing dialog ────────────────────
        currentRide = null;

        // ── Show 'Ride Finished' summary dialog with fare ────────────────────
        String fareStr = String.format(Locale.getDefault(), "Rs. %.0f", finalFare);
        String distStr = distanceKm > 0
                ? String.format(Locale.getDefault(), "%.1f km", distanceKm)
                : "N/A";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🎉 Ride Completed!")
                .setMessage("Trip finished successfully.\n\n"
                        + "Distance:   " + distStr + "\n"
                        + "Total Fare: " + fareStr + "\n\n"
                        + "Please collect payment from the passenger.")
                .setPositiveButton("Done", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();

        // Reset map and panels
        customerInfo.setVisibility(View.GONE);
        rideStatusButton.setVisibility(View.GONE);
        mMap.clear();
        updateRequestsVisibility();
    }

    private void calculateRouteToPickup() {
        if (currentRide == null || lastLocation == null) return;
        
        LatLng driverLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        LatLng pickupLatLng = new LatLng(currentRide.getPickupLat(), currentRide.getPickupLng());
        
        routeHelper.getRoute(driverLatLng, pickupLatLng);
        
        mMap.addMarker(new MarkerOptions()
                .position(pickupLatLng)
                .title("Pickup")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
    }

    private void calculateRouteToDestination() {
        if (currentRide == null || lastLocation == null) return;
        
        LatLng driverLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        LatLng destLatLng = new LatLng(currentRide.getDestLat(), currentRide.getDestLng());
        
        routeHelper.getRoute(driverLatLng, destLatLng);
        
        mMap.clear();
        mMap.addMarker(new MarkerOptions()
                .position(destLatLng)
                .title("Destination")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
    }

    @Override
    public void onRouteSuccess(ArrayList<LatLng> path, double distance, int duration) {
        for (Polyline polyline : polylines) {
            polyline.remove();
        }
        polylines.clear();
        
        PolylineOptions polylineOptions = RouteHelper.createPolylineOptions(path, 
                ContextCompat.getColor(this, R.color.map_route));
        
        polylines.add(mMap.addPolyline(polylineOptions));
    }

    @Override
    public void onRouteFailure(String error) {
        Log.e(TAG, "Route error: " + error);
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
        startActivity(mapIntent);
    }

    private void callCustomer() {
        if (currentRide == null || currentRide.getCustomerPhone() == null) return;
        
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + currentRide.getCustomerPhone()));
        startActivity(intent);
    }

    private void showCancelDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Ride")
                .setMessage("Are you sure you want to cancel this ride?")
                .setPositiveButton("Yes", (dialog, which) -> cancelRide())
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelRide() {
        if (currentRide == null) return;
        
        if (currentRide.getRideId() != null) {
            DatabaseReference rideRef = FirebaseDatabase.getInstance().getReference("ride_info").child(currentRide.getRideId());
            rideRef.child("status").setValue("cancelled");
            
            // Notify Rider
            FirebaseDatabase.getInstance().getReference("customerRequest")
                .child(currentRide.getRideId()).child("status").setValue("cancelled");
        }
        
        Toast.makeText(this, "Ride cancelled", Toast.LENGTH_SHORT).show();
        
        currentRide = null;
        customerInfo.setVisibility(View.GONE);
        rideStatusButton.setVisibility(View.GONE);
        mMap.clear();
        updateRequestsVisibility();
    }

    private void loadDriverData() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverDataRef = FirebaseDatabase.getInstance().getReference("Users").child("Drivers").child(userId);
        
        driverDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentDriver = snapshot.getValue(User.class);
                    if (driverNameHeader != null && currentDriver.getName() != null) {
                        driverNameHeader.setText(currentDriver.getName());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkForActiveRide() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ridesRef = FirebaseDatabase.getInstance().getReference("ride_info");
        
        ridesRef.orderByChild("driverId").equalTo(driverId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot rideSnapshot : snapshot.getChildren()) {
                    RideRequest ride = rideSnapshot.getValue(RideRequest.class);
                    if (ride != null && !"completed".equals(ride.getStatus()) && !"cancelled".equals(ride.getStatus())) {
                        
                        // Safety Check: Ignore highly corrupted test rides
                        if (ride.getCustomerName() == null && ride.getRideId() == null && rideSnapshot.getKey() != null) {
                            rideSnapshot.getRef().removeValue(); // Automatically clean ghost requests
                            continue;
                        }

                        currentRide = ride;
                        if (currentRide.getRideId() == null) {
                            currentRide.setRideId(rideSnapshot.getKey());
                        }

                        showCurrentRideUI();
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupPassengerList() {
        if (passengerRecyclerView != null) {
            passengerRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

        if (btnFindMorePassengers != null) {
            btnFindMorePassengers.setOnClickListener(v -> findMorePassengers());
        }
    }

    private void findMorePassengers() {
        // Shared ride logic
        Toast.makeText(this, "Searching for nearby passengers...", Toast.LENGTH_SHORT).show();
    }

    private void logout() {
        goOffline();
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(DriverMapActivity.this, LauncherActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        
        if (checkLocationPermission()) {
            mMap.setMyLocationEnabled(true);
        }
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                lastLocation = location;
                
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                
                if (!zoomUpdated) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    zoomUpdated = true;
                }
                
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                
                if (currentRide != null && currentRide.getRideId() != null) {
                    // Update driver location in ride info
                    DatabaseReference rideRef = FirebaseDatabase.getInstance().getReference("ride_info").child(currentRide.getRideId());
                    rideRef.child("driverLat").setValue(location.getLatitude());
                    rideRef.child("driverLng").setValue(location.getLongitude());
                }
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void listenForFixedRouteBookings(String driverId) {
        DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference().child("FixedRouteBookings").child(driverId);
        bookingsRef.addChildEventListener(new com.google.firebase.database.ChildEventListener() {
            private long startTime = System.currentTimeMillis();
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                if (timestamp != null && timestamp > startTime) {
                    String riderName = snapshot.child("riderName").getValue(String.class);
                    if (riderName != null) {
                        new android.app.AlertDialog.Builder(DriverMapActivity.this)
                               .setTitle("New Carpooling Booking!")
                               .setMessage(riderName + " has booked a seat on your fixed route.")
                               .setPositiveButton("View", (dialog, which) -> {
                                   startActivity(new Intent(DriverMapActivity.this, ViewFixedRouteBookingsActivity.class));
                               })
                               .setNegativeButton("Dismiss", null)
                               .show();
                    }
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateDriverEarnings(String driverId, double fare, double distance) {
        DatabaseReference driverEarningsRef = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(driverId).child("earnings");
        
        driverEarningsRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData currentData) {
                double available = 0;
                double total = 0;
                
                double daily = 0;
                double monthly = 0;
                long lastUpdate = 0;

                if (currentData.child("available").getValue() != null) {
                    available = Double.parseDouble(currentData.child("available").getValue().toString());
                }
                if (currentData.child("total").getValue() != null) {
                    total = Double.parseDouble(currentData.child("total").getValue().toString());
                }

                if (currentData.child("daily").getValue() != null) {
                    daily = Double.parseDouble(currentData.child("daily").getValue().toString());
                }
                if (currentData.child("monthly").getValue() != null) {
                    monthly = Double.parseDouble(currentData.child("monthly").getValue().toString());
                }
                if (currentData.child("lastUpdate").getValue() != null) {
                    lastUpdate = Long.parseLong(currentData.child("lastUpdate").getValue().toString());
                }

                // Check for day/month reset
                long now = System.currentTimeMillis();
                java.util.Calendar calNow = java.util.Calendar.getInstance();
                java.util.Calendar calLast = java.util.Calendar.getInstance();
                calNow.setTimeInMillis(now);
                calLast.setTimeInMillis(lastUpdate);

                boolean isNewDay = calNow.get(java.util.Calendar.DAY_OF_YEAR) != calLast.get(java.util.Calendar.DAY_OF_YEAR) 
                                   || calNow.get(java.util.Calendar.YEAR) != calLast.get(java.util.Calendar.YEAR);
                boolean isNewMonth = calNow.get(java.util.Calendar.MONTH) != calLast.get(java.util.Calendar.MONTH)
                                     || calNow.get(java.util.Calendar.YEAR) != calLast.get(java.util.Calendar.YEAR);

                if (isNewDay) daily = 0;
                if (isNewMonth) monthly = 0;

                currentData.child("available").setValue(available + fare);
                currentData.child("total").setValue(total + fare);
                currentData.child("daily").setValue(daily + fare);
                currentData.child("monthly").setValue(monthly + fare);
                currentData.child("lastUpdate").setValue(now);
                
                return com.google.firebase.database.Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) Log.e(TAG, "Earnings update failed", error.toException());
            }
        });

        DatabaseReference driverStatsRef = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(driverId).child("stats");
        
        driverStatsRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData currentData) {
                int totalRides = 0;
                double totalDistance = 0;
                
                if (currentData.child("totalRides").getValue() != null) {
                    totalRides = Integer.parseInt(currentData.child("totalRides").getValue().toString());
                }
                if (currentData.child("totalDistance").getValue() != null) {
                    totalDistance = Double.parseDouble(currentData.child("totalDistance").getValue().toString());
                }
                
                currentData.child("totalRides").setValue(totalRides + 1);
                currentData.child("totalDistance").setValue(totalDistance + distance);
                
                return com.google.firebase.database.Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) Log.e(TAG, "Stats update failed", error.toException());
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GPS_SETTINGS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // GPS enabled, refresh location updates if already online
                if (workingSwitch != null && workingSwitch.isChecked()) {
                    goOnline();
                }
            } else {
                Toast.makeText(this, "Please turn on location to receive ride requests", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (voiceHelper != null) {
            voiceHelper.shutdown();
        }
        super.onDestroy();
    }
}