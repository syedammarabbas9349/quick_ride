package com.example.quickride.driver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
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
import androidx.cardview.widget.CardView;
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

/**
 * Driver Map Activity with simple drawer implementation
 */
public class DriverMapActivity extends AppCompatActivity
        implements OnMapReadyCallback, RouteHelper.RouteCallback {

    // Constants
    private static final int MAX_SEARCH_DISTANCE = 20;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    // UI Components
    private GoogleMap mMap;
    private DrawerLayout drawer;
    private Toolbar toolbar;
    private ListView drawerList;
    private ImageView drawerButton;
    private ImageView customerProfileImage;
    private Switch workingSwitch;
    private Button rideStatusButton;
    private Button fabMaps, fabCall;
    private Button cancelButton;
    private TextView customerName, pickupAddress, driverNameHeader, driverStatusHeader;
    private LinearLayout customerInfo, bringUpBottomLayout;
    private CardView bottomSheet;
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
    private DrawerAdapter drawerAdapter;

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
            R.drawable.ic_payment_24dp,
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
        setupDrawer();
        setupFirebase();
        setupLocation();
        setupMap();
        setupRecyclerView();
        setupBottomSheet();
        setupListeners();
        loadDriverData();
        checkForActiveRide();
    }

    private void initializeViews() {
        drawer = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        drawerList = findViewById(R.id.drawer_list);
        drawerButton = findViewById(R.id.drawerButton);
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

        // Header views
        View headerView = getLayoutInflater().inflate(R.layout.nav_header_driver, null);
        driverNameHeader = headerView.findViewById(R.id.driverNameDrawer);
        driverStatusHeader = headerView.findViewById(R.id.driverStatusDrawer);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        drawerButton.setOnClickListener(v -> drawer.openDrawer(Gravity.LEFT));
    }

    private void setupDrawer() {
        // Get drawer items from resources
        drawerItems = getResources().getStringArray(R.array.drawer_items_driver);

        // Set up adapter
        drawerAdapter = new DrawerAdapter(this, drawerItems, drawerIcons);
        drawerList.setAdapter(drawerAdapter);

        // Add header to ListView
        drawerList.addHeaderView(getLayoutInflater().inflate(R.layout.nav_header_driver, null));

        // Set item click listener
        drawerList.setOnItemClickListener((parent, view, position, id) -> {
            // Adjust position because of header
            int actualPosition = position - 1;

            if (actualPosition >= 0) {
                drawerAdapter.setSelectedPosition(actualPosition);

                switch (actualPosition) {
                    case 0: // History
                        startActivity(new Intent(this, HistoryActivity.class)
                                .putExtra("userType", "Drivers"));
                        break;
                    case 1: // Earnings
                        Toast.makeText(this, "Earnings clicked", Toast.LENGTH_SHORT).show();
                        break;
                    case 2: // Payout
                        startActivity(new Intent(this, PayoutActivity.class));
                        break;
                    case 3: // Settings
                        startActivity(new Intent(this, DriverSettingsActivity.class));
                        break;
                    case 4: // Help
                        Toast.makeText(this, "Help clicked", Toast.LENGTH_SHORT).show();
                        break;
                    case 5: // Logout
                        showLogoutDialog();
                        break;
                }
            }

            drawer.closeDrawer(GravityCompat.START);
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

    private void setupFirebase() {
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
    }

    private void setupLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupRecyclerView() {
        requestAdapter = new RideRequestAdapter(requestList, new RideRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(RideRequest request, int position) {
                acceptRide(request);
            }

            @Override
            public void onDecline(RideRequest request, int position) {
                requestList.remove(position);
                requestAdapter.notifyItemRemoved(position);
                if (requestList.isEmpty()) {
                    noRequestsText.setVisibility(View.VISIBLE);
                    requestsRecyclerView.setVisibility(View.GONE);
                }
            }
        });

        requestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        requestsRecyclerView.setAdapter(requestAdapter);
    }

    private void setupBottomSheet() {
        bottomSheetView = findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        bringUpBottomLayout.setOnClickListener(v -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

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

    private void setupListeners() {
        workingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                goOnline();
            } else {
                goOffline();
            }
        });

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

        fabMaps.setOnClickListener(v -> openMaps());
        fabCall.setOnClickListener(v -> callCustomer());
        cancelButton.setOnClickListener(v -> showCancelDialog());
    }

    private void goOnline() {
        if (!checkLocationPermission()) {
            workingSwitch.setChecked(false);
            return;
        }

        workingSwitch.setChecked(true);
        startLocationUpdates();

        if (mMap != null) {
            if (checkLocationPermission()) {
                mMap.setMyLocationEnabled(true);
            }
        }

        if (driverStatusHeader != null) {
            driverStatusHeader.setText(R.string.online);
        }
        Snackbar.make(drawer, R.string.you_are_online, Snackbar.LENGTH_SHORT).show();
    }

    private void goOffline() {
        workingSwitch.setChecked(false);
        stopLocationUpdates();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        geoFireWorking.removeLocation(userId);

        if (driverStatusHeader != null) {
            driverStatusHeader.setText(R.string.offline);
        }
        Snackbar.make(drawer, R.string.you_are_offline, Snackbar.LENGTH_SHORT).show();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) return;

            for (Location location : locationResult.getLocations()) {
                lastLocation = location;

                // Update driver location in Firebase
                if (workingSwitch.isChecked()) {
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    geoFireWorking.setLocation(userId,
                            new GeoLocation(location.getLatitude(), location.getLongitude()));

                    // Update last updated timestamp
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("last_updated", ServerValue.TIMESTAMP);
                    driverRef.updateChildren(updates);
                }

                // Update map camera if needed
                if (!zoomUpdated) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(location.getLatitude(), location.getLongitude()), 16));
                    zoomUpdated = true;
                }

                // Start searching for requests
                if (!started && workingSwitch.isChecked()) {
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
        if (lastLocation == null) return;

        DatabaseReference requestsLocation = FirebaseDatabase.getInstance()
                .getReference().child("customer_requests");

        GeoFire geoFire = new GeoFire(requestsLocation);
        geoQuery = geoFire.queryAtLocation(
                new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()),
                MAX_SEARCH_DISTANCE);

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!workingSwitch.isChecked() || currentRide != null) return;

                // Check if request already in list
                for (RideRequest ride : requestList) {
                    if (ride.getRequestId().equals(key)) return;
                }

                fetchRequestDetails(key);
            }

            @Override
            public void onKeyExited(String key) {}

            @Override
            public void onKeyMoved(String key, GeoLocation location) {}

            @Override
            public void onGeoQueryReady() {}

            @Override
            public void onGeoQueryError(DatabaseError error) {}
        });
    }

    private void fetchRequestDetails(String requestId) {
        rideInfoRef.child(requestId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || currentRide != null) return;

                RideRequest request = snapshot.getValue(RideRequest.class);
                if (request == null) return;

                // Check if vehicle type matches
                if (!request.getVehicleType().equals(currentDriver.getVehicleType())) return;

                // Check if request is still pending
                if (!"pending".equals(request.getStatus())) return;

                request.setRequestId(requestId);
                requestList.add(request);
                requestAdapter.notifyItemInserted(requestList.size() - 1);

                if (noRequestsText != null) {
                    noRequestsText.setVisibility(View.GONE);
                    requestsRecyclerView.setVisibility(View.VISIBLE);
                }

                // Play notification sound
                playNotificationSound();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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
        currentRide = request;
        currentRide.setDriverId(currentDriver.getId());
        currentRide.setDriverName(currentDriver.getName());
        currentRide.setDriverPhone(currentDriver.getPhone());
        currentRide.setStatus("accepted");
        currentRide.setAcceptedTime(System.currentTimeMillis());

        // Update in Firebase
        Map<String, Object> updates = new HashMap<>();
        updates.put("driverId", currentDriver.getId());
        updates.put("driverName", currentDriver.getName());
        updates.put("driverPhone", currentDriver.getPhone());
        updates.put("status", "accepted");
        updates.put("acceptedTime", System.currentTimeMillis());

        rideInfoRef.child(request.getRequestId()).updateChildren(updates);

        // Update driver's current ride
        driverRef.child("currentRideId").setValue(request.getRequestId());

        // Clear request list
        requestList.clear();
        requestAdapter.notifyDataSetChanged();
        if (noRequestsText != null) {
            noRequestsText.setVisibility(View.VISIBLE);
            requestsRecyclerView.setVisibility(View.GONE);
        }

        // Show customer info
        showCustomerInfo();

        // Start listening for ride updates
        listenForRideUpdates();

        // Calculate route to pickup
        calculateRouteToPickup();
    }

    private void showCustomerInfo() {
        if (currentRide == null) return;

        customerInfo.setVisibility(View.VISIBLE);
        customerName.setText(currentRide.getCustomerName());
        pickupAddress.setText(currentRide.getPickupAddress());

        // Load customer image if available
        if (currentRide.getCustomerImageUrl() != null &&
                !currentRide.getCustomerImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentRide.getCustomerImageUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(customerProfileImage);
        }

        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void listenForRideUpdates() {
        if (currentRide == null) return;

        rideStatusListener = rideInfoRef.child(currentRide.getRequestId())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;

                        String status = snapshot.child("status").getValue(String.class);
                        if (status == null) return;

                        currentRide.setStatus(status);

                        switch (status) {
                            case "cancelled":
                            case "completed":
                                endRide();
                                break;
                            case "started":
                                rideStatusButton.setText(R.string.complete_ride);
                                break;
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void calculateRouteToPickup() {
        if (lastLocation == null || currentRide == null) return;

        LatLng origin = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        LatLng destination = new LatLng(
                currentRide.getPickupLat(),
                currentRide.getPickupLng());

        routeHelper.getRoute(origin, destination);
    }

    private void calculateRouteToDestination() {
        if (lastLocation == null || currentRide == null) return;

        LatLng origin = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        LatLng destination = new LatLng(
                currentRide.getDestLat(),
                currentRide.getDestLng());

        routeHelper.getRoute(origin, destination);
    }

    private void updateRideStatus(String status) {
        if (currentRide == null) return;

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

        rideInfoRef.child(currentRide.getRequestId()).updateChildren(updates);
    }

    private void updateRideDistance(Location newLocation) {
        if (lastLocation == null || currentRide == null) return;

        float distanceDelta = lastLocation.distanceTo(newLocation) / 1000; // in km
        double totalDistance = currentRide.getDistance() + distanceDelta;

        Map<String, Object> updates = new HashMap<>();
        updates.put("distance", totalDistance);

        rideInfoRef.child(currentRide.getRequestId()).updateChildren(updates);
    }

    private void completeRide() {
        if (currentRide == null) return;

        double fare = calculateFare(currentRide.getDistance());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "completed");
        updates.put("completedTime", System.currentTimeMillis());
        updates.put("fare", fare);

        rideInfoRef.child(currentRide.getRequestId()).updateChildren(updates);

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
        if (currentRide == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "cancelled");
        updates.put("cancelledTime", System.currentTimeMillis());
        updates.put("cancelledBy", "driver");

        rideInfoRef.child(currentRide.getRequestId()).updateChildren(updates);
        endRide();
    }

    private void endRide() {
        if (rideStatusListener != null && currentRide != null) {
            rideInfoRef.child(currentRide.getRequestId()).removeEventListener(rideStatusListener);
        }

        // Clear driver's current ride
        driverRef.child("currentRideId").removeValue();

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
        customerInfo.setVisibility(View.GONE);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        rideStatusButton.setText(R.string.pickup_customer);

        // Clear map and restart search
        mMap.clear();
        zoomUpdated = false;
        started = false;

        if (workingSwitch.isChecked()) {
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
        if (currentRide == null || currentRide.getCustomerPhone() == null) return;

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
        driverRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                currentDriver.setName(snapshot.child("name").getValue(String.class));
                currentDriver.setPhone(snapshot.child("phone").getValue(String.class));
                currentDriver.setCar(snapshot.child("car").getValue(String.class));
                currentDriver.setVehicleType(snapshot.child("vehicleType").getValue(String.class));

                Boolean active = snapshot.child("active").getValue(Boolean.class);
                if (active != null && !active) {
                    workingSwitch.setChecked(false);
                    workingSwitch.setEnabled(false);
                    Toast.makeText(DriverMapActivity.this,
                            R.string.not_approved, Toast.LENGTH_LONG).show();
                }

                if (driverNameHeader != null) {
                    driverNameHeader.setText(currentDriver.getName());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkForActiveRide() {
        driverRef.child("currentRideId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String rideId = snapshot.getValue(String.class);
                if (rideId == null || rideId.isEmpty()) return;

                rideInfoRef.child(rideId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) return;

                        currentRide = dataSnapshot.getValue(RideRequest.class);
                        if (currentRide == null) return;

                        currentRide.setRequestId(rideId);

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
                if (workingSwitch.isChecked()) {
                    startLocationUpdates();
                }
            } else {
                Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_LONG).show();
                workingSwitch.setChecked(false);
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Apply custom map style (optional)
        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
        } catch (Exception e) {
            // Use default style
        }

        if (checkLocationPermission()) {
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRouteSuccess(ArrayList<LatLng> path, double distance, int duration) {
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
        Snackbar.make(drawer, "Route error: " + error, Snackbar.LENGTH_SHORT).show();
    }

    private void logout() {
        goOffline();
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LauncherActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();

        if (rideStatusListener != null && currentRide != null) {
            rideInfoRef.child(currentRide.getRequestId()).removeEventListener(rideStatusListener);
        }

        if (geoQuery != null) {
            geoQuery.removeAllListeners();
        }
    }
}