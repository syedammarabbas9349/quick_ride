package com.example.quickride.customer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.example.quickride.adapters.TypeAdapter;
import com.example.quickride.auth.LauncherActivity;
import com.example.quickride.history.HistoryActivity;
import com.example.quickride.models.RideRequest;
import com.example.quickride.models.ServiceType;
import com.example.quickride.payment.AddPaymentActivity;
import com.example.quickride.payment.PaymentActivity;
import com.example.quickride.utils.LocationHelper;
import com.example.quickride.utils.NotificationHelper;
import com.example.quickride.utils.RouteHelper;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomerMapActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        RouteHelper.RouteCallback {

    private static final String TAG = "CustomerMapActivity";

    // Constants
    private static final int TIMEOUT_MILLISECONDS = 20000;
    private static final int CANCEL_OPTION_MILLISECONDS = 10000;
    private static final int AUTOCOMPLETE_REQUEST_CODE_TO = 1;
    private static final int AUTOCOMPLETE_REQUEST_CODE_FROM = 2;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    // UI Components
    private GoogleMap mMap;
    private DrawerLayout drawer;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private CardView mContainer;
    private LinearLayout mDriverInfo, mRadioLayout, mLocation, mLooking;
    private TextView mTimeout;
    private ImageView mDriverProfileImage, mDrawerButton;
    private TextView mDriverName, mDriverCar, mDriverLicense, mRatingText;
    private TextView autocompleteFragmentTo, autocompleteFragmentFrom;
    private Button mRequest, mSettings, mLogout;
    private Button mCallDriver, mCancel, mCancelTimeout;
    private FloatingActionButton mCurrentLocation;
    private RecyclerView mRecyclerView;
    private TypeAdapter mAdapter;

    // Data
    private RideRequest mCurrentRide;
    private LocationHelper mLocationHelper;
    private RouteHelper mRouteHelper;
    private NotificationHelper mNotificationHelper;

    private LatLng pickupLatLng, destinationLatLng;
    private String pickupAddress, destinationAddress;
    private ArrayList<ServiceType> typeArrayList = new ArrayList<>();
    private List<Marker> driverMarkers = new ArrayList<>();
    private Marker mDriverMarker, pickupMarker, destinationMarker, mDraggableDestinationMarker;
    private List<Polyline> polylines = new ArrayList<>();
    private ArrayList<Double> routeData;

    // State
    private boolean requestBol = false;
    private boolean driverFound = false;
    private boolean zoomUpdated = false;
    private boolean getDriversAroundStarted = false;
    private boolean isDraggingMarker = false;
    private int bottomSheetStatus = 1;

    // Handlers
    private Handler cancelHandler = new Handler();
    private Handler timeoutHandler = new Handler();
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;
    private ValueEventListener rideStatusListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Log.d(TAG, "onCreate started");
            setContentView(R.layout.activity_customer_map);
            Log.d(TAG, "Layout inflated successfully");

            initializeViews();
            setupToolbar();
            setupNavigationDrawer();
            initializeHelpers();
            setupPlaces();
            setupRecyclerView();
            setupLocation();
            checkForActiveRide();
            setupMap();

            Log.d(TAG, "onCreate completed");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initializeViews() {
        drawer = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        navigationView = findViewById(R.id.nav_view);
        mContainer = findViewById(R.id.container_card);

        mDriverInfo = findViewById(R.id.driverInfo);
        mRadioLayout = findViewById(R.id.radioLayout);
        mLocation = findViewById(R.id.location_layout);
        mLooking = findViewById(R.id.looking_layout);
        mTimeout = findViewById(R.id.timeout_layout);

        mDriverProfileImage = findViewById(R.id.driverProfileImage);
        mDriverName = findViewById(R.id.driverName);
        mDriverCar = findViewById(R.id.driverCar);
        mDriverLicense = findViewById(R.id.driverPlate);
        mRatingText = findViewById(R.id.ratingText);

        autocompleteFragmentTo = findViewById(R.id.place_to);
        autocompleteFragmentFrom = findViewById(R.id.place_from);

        mDrawerButton = findViewById(R.id.drawerButton);

        mRequest = findViewById(R.id.request);
        mSettings = findViewById(R.id.settings);
        mLogout = findViewById(R.id.logout);

        mCallDriver = findViewById(R.id.phone);
        mCancel = findViewById(R.id.cancel);
        mCancelTimeout = findViewById(R.id.cancel_looking);
        mCurrentLocation = findViewById(R.id.current_location);

        mRecyclerView = findViewById(R.id.recyclerView);

        // SETTINGS BUTTON
        if (mSettings != null) {
            mSettings.setOnClickListener(v -> {
                Log.d(TAG,"Settings clicked");
                Intent intent = new Intent(CustomerMapActivity.this, CustomerSettingsActivity.class);
                startActivity(intent);
            });
        }

        // LOGOUT BUTTON
        if (mLogout != null) {
            mLogout.setOnClickListener(v -> {
                Log.d(TAG,"Logout clicked");
                logout();
            });
        }

        // REQUEST BUTTON
        if (mRequest != null) {
            mRequest.setOnClickListener(v -> {
                if (!requestBol) {
                    startRideRequest();
                } else {
                    cancelRide();
                }
            });
        }

        // CALL DRIVER BUTTON
        if (mCallDriver != null) {
            mCallDriver.setOnClickListener(v -> callDriver());
        }

        // CANCEL BUTTON
        if (mCancel != null) {
            mCancel.setOnClickListener(v -> cancelRide());
        }

        // CANCEL TIMEOUT BUTTON
        if (mCancelTimeout != null) {
            mCancelTimeout.setOnClickListener(v -> cancelRide());
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle =
                new ActionBarDrawerToggle(
                        this,
                        drawer,
                        toolbar,
                        R.string.navigation_drawer_open,
                        R.string.navigation_drawer_close);

        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mDrawerButton.setOnClickListener(v ->
                drawer.openDrawer(GravityCompat.START));
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(this);
        loadUserProfile();
    }

    private void initializeHelpers() {
        mLocationHelper = new LocationHelper(this);
        mRouteHelper = new RouteHelper(this, getString(R.string.google_maps_key));
        mRouteHelper.setCallback(this);
        mNotificationHelper = NotificationHelper.getInstance(this);
        mCurrentRide = new RideRequest();
    }

    private void setupPlaces() {
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }

        autocompleteFragmentTo.setOnClickListener(v -> {
            if (requestBol) {
                Toast.makeText(this, "Cannot change destination during active ride", Toast.LENGTH_SHORT).show();
                return;
            }
            openPlaceAutocomplete(AUTOCOMPLETE_REQUEST_CODE_TO);
        });

        autocompleteFragmentFrom.setOnClickListener(v -> {
            if (requestBol) {
                Toast.makeText(this, "Cannot change pickup during active ride", Toast.LENGTH_SHORT).show();
                return;
            }
            openPlaceAutocomplete(AUTOCOMPLETE_REQUEST_CODE_FROM);
        });
    }

    private void openPlaceAutocomplete(int requestCode) {
        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY,
                Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
                .build(this);
        startActivityForResult(intent, requestCode);
    }

    private void setupRecyclerView() {
        routeData = new ArrayList<>();
        typeArrayList = getTypeList();

        mRecyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.HORIZONTAL,
                        false));

        mAdapter = new TypeAdapter(
                typeArrayList,
                this,
                routeData,
                (type, position) -> {
                    Log.d(TAG,"Vehicle selected: " + type.getName());
                });

        mRecyclerView.setAdapter(mAdapter);
    }

    private ArrayList<ServiceType> getTypeList() {
        ArrayList<ServiceType> types = new ArrayList<>();

        ServiceType economy = new ServiceType("economy", "Economy", "economy", 15.0, 4, R.drawable.ic_economy_car);
        economy.setBaseFare(50.0);
        economy.setMinimumFare(100.0);
        types.add(economy);

        ServiceType premium = new ServiceType("premium", "Premium", "premium", 25.0, 4, R.drawable.ic_premium_car);
        premium.setBaseFare(80.0);
        premium.setMinimumFare(150.0);
        types.add(premium);

        ServiceType xl = new ServiceType("xl", "XL", "xl", 35.0, 6, R.drawable.ic_suv);
        xl.setBaseFare(100.0);
        xl.setMinimumFare(200.0);
        types.add(xl);

        return types;
    }

    private void setupLocation() {
        if (mLocationHelper.hasLocationPermission()) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        mCurrentLocation.setOnClickListener(v -> setCurrentLocationAsPickup());
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(2000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationClient.requestLocationUpdates(
                locationRequest, mLocationCallback, Looper.myLooper());
    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) return;

            for (Location location : locationResult.getLocations()) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                if (!zoomUpdated && mMap != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f));
                    zoomUpdated = true;
                }

                if (!getDriversAroundStarted) {
                    getDriversAround();
                }
            }
        }
    };

    private void setCurrentLocationAsPickup() {
        if (mLocationHelper.getCurrentLocation() == null) {
            // Try to get location one more time
            if (mLocationHelper.hasLocationPermission()) {
                FusedLocationProviderClient fusedLocationClient =
                        LocationServices.getFusedLocationProviderClient(this);

                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                        if (location != null) {
                            pickupLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            fetchAddressFromLocation(pickupLatLng, false);
                            updatePickupMarker();
                            mCurrentLocation.setImageResource(R.drawable.ic_location_on_primary_24dp);

                            if (destinationLatLng != null) {
                                calculateRoute();
                                showBottomSheet(2);
                            }
                        }
                    });
                }
            }
            return;
        }

        pickupLatLng = mLocationHelper.getCurrentLocation();
        fetchAddressFromLocation(pickupLatLng, false);
        updatePickupMarker();

        if (mCurrentLocation != null) {
            mCurrentLocation.setImageResource(R.drawable.ic_location_on_primary_24dp);
        }

        if (destinationLatLng != null) {
            calculateRoute();
            showBottomSheet(2);
        }
    }

    private void fetchAddressFromLocation(LatLng latLng, boolean isDestination) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(
                    latLng.latitude, latLng.longitude, 1);

            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                String fullAddress = address.getAddressLine(0);

                if (isDestination) {
                    destinationAddress = fullAddress;
                    if (autocompleteFragmentTo != null) {
                        autocompleteFragmentTo.setText(fullAddress);
                    }

                    // Update or create destination marker
                    if (destinationMarker != null) {
                        destinationMarker.setPosition(latLng);
                    } else if (mMap != null) {
                        destinationMarker = mMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title("Destination")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    }
                } else {
                    pickupAddress = fullAddress;
                    if (autocompleteFragmentFrom != null) {
                        autocompleteFragmentFrom.setText(fullAddress);
                    }

                    // Update or create pickup marker
                    if (pickupMarker != null) {
                        pickupMarker.setPosition(latLng);
                    } else if (mMap != null) {
                        pickupMarker = mMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title("Pickup")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder error", e);
            // Fallback to coordinates
            String coordText = String.format(Locale.getDefault(),
                    "%.6f, %.6f", latLng.latitude, latLng.longitude);

            if (isDestination) {
                destinationAddress = coordText;
                if (autocompleteFragmentTo != null) {
                    autocompleteFragmentTo.setText(coordText);
                }
            } else {
                pickupAddress = coordText;
                if (autocompleteFragmentFrom != null) {
                    autocompleteFragmentFrom.setText(coordText);
                }
            }
        }
    }

    private void fetchAddressFromLocation(LatLng latLng) {
        fetchAddressFromLocation(latLng, false);
    }

    private void updatePickupMarker() {
        if (mMap == null || pickupLatLng == null) return;

        if (pickupMarker != null) pickupMarker.remove();
        pickupMarker = mMap.addMarker(new MarkerOptions()
                .position(pickupLatLng)
                .title("Pickup")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "Map is ready");

        if (mLocationHelper.hasLocationPermission()) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                mMap.setMyLocationEnabled(true);
            }
            startLocationUpdates();
        }

        // Setup marker drag listener
        setupMapMarkerDragListener();

        // Auto-set current location as pickup after map is ready
        new Handler().postDelayed(() -> {
            setCurrentLocationAsPickup();
        }, 1000);
    }

    private void setupMapMarkerDragListener() {
        if (mMap == null) return;

        mMap.setOnMapLongClickListener(latLng -> {
            if (requestBol) {
                Toast.makeText(this, "Cannot change destination during active ride", Toast.LENGTH_SHORT).show();
                return;
            }

            // Remove existing draggable marker
            if (mDraggableDestinationMarker != null) {
                mDraggableDestinationMarker.remove();
            }

            // Add new draggable marker at long-pressed location
            mDraggableDestinationMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Destination")
                    .snippet("Drag to adjust or type address")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    .draggable(true));

            // Set as destination
            destinationLatLng = latLng;

            // Reverse geocode to get address
            fetchAddressFromLocation(latLng, true);

            if (pickupLatLng != null) {
                calculateRoute();
                showBottomSheet(2);
            }
        });

        // Handle marker drag events
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                if (marker.equals(mDraggableDestinationMarker)) {
                    isDraggingMarker = true;
                }
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                // Optional: Show temporary coordinates while dragging
                if (marker.equals(mDraggableDestinationMarker) && autocompleteFragmentTo != null) {
                    LatLng position = marker.getPosition();
                    autocompleteFragmentTo.setText(String.format("Moving to: %.4f, %.4f",
                            position.latitude, position.longitude));
                }
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                if (marker.equals(mDraggableDestinationMarker)) {
                    isDraggingMarker = false;
                    LatLng finalPosition = marker.getPosition();

                    // Update destination
                    destinationLatLng = finalPosition;

                    // Get address for the dropped location
                    fetchAddressFromLocation(finalPosition, true);

                    // Update route if pickup is set
                    if (pickupLatLng != null) {
                        calculateRoute();
                        showBottomSheet(2);
                    }

                    Toast.makeText(CustomerMapActivity.this,
                            "Destination updated", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void getDriversAround() {
        if (mLocationHelper.getCurrentLocation() == null) return;

        getDriversAroundStarted = true;
        LatLng current = mLocationHelper.getCurrentLocation();

        DatabaseReference driversLocation = FirebaseDatabase.getInstance()
                .getReference().child("driversWorking");

        GeoFire geoFire = new GeoFire(driversLocation);
        GeoQuery geoQuery = geoFire.queryAtLocation(
                new GeoLocation(current.latitude, current.longitude), 10000);

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                addDriverMarker(key, location);
            }

            @Override
            public void onKeyExited(String key) {
                removeDriverMarker(key);
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                updateDriverMarker(key, location);
            }

            @Override
            public void onGeoQueryReady() {}

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e(TAG, "GeoQuery error: " + error.getMessage());
            }
        });
    }

    private void addDriverMarker(String driverId, GeoLocation location) {
        if (mCurrentRide == null || mCurrentRide.getDriverId() != null || mMap == null) return;

        LatLng driverLatLng = new LatLng(location.latitude, location.longitude);
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(driverLatLng)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car_top))
                .title(driverId));

        marker.setTag(driverId);
        driverMarkers.add(marker);
    }

    private void removeDriverMarker(String driverId) {
        for (int i = 0; i < driverMarkers.size(); i++) {
            Marker marker = driverMarkers.get(i);
            if (marker.getTag() != null && marker.getTag().equals(driverId)) {
                marker.remove();
                driverMarkers.remove(i);
                break;
            }
        }
    }

    private void updateDriverMarker(String driverId, GeoLocation location) {
        for (Marker marker : driverMarkers) {
            if (marker.getTag() != null && marker.getTag().equals(driverId)) {
                marker.setPosition(new LatLng(location.latitude, location.longitude));
                break;
            }
        }
    }

    private void startRideRequest() {
        if (pickupLatLng == null || destinationLatLng == null) {
            Toast.makeText(this, "Please set pickup and destination", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAdapter == null || mAdapter.getSelectedItem() == null) {
            Toast.makeText(this, "Please select a vehicle type", Toast.LENGTH_SHORT).show();
            return;
        }

        showBottomSheet(3);

        mCurrentRide.setCustomerId(FirebaseAuth.getInstance().getCurrentUser().getUid());
        mCurrentRide.setPickupLat(pickupLatLng.latitude);
        mCurrentRide.setPickupLng(pickupLatLng.longitude);
        mCurrentRide.setPickupAddress(pickupAddress);
        mCurrentRide.setDestLat(destinationLatLng.latitude);
        mCurrentRide.setDestLng(destinationLatLng.longitude);
        mCurrentRide.setDestinationAddress(destinationAddress);
        mCurrentRide.setVehicleType(mAdapter.getSelectedItem().getVehicleType());
        mCurrentRide.setFare(calculateFare());
        mCurrentRide.setStatus("pending");
        mCurrentRide.setTimestamp(System.currentTimeMillis());

        saveRideRequest();
        setupRideTimeout();
        requestListener();
    }

    private double calculateFare() {
        double baseFare = 50;
        ServiceType selectedType = mAdapter.getSelectedItem();
        double perKmRate = selectedType != null ? selectedType.getPricePerKm() : 15.0;
        double distance = routeData != null && routeData.size() > 0 ? routeData.get(0) : 5.0;
        return baseFare + (distance * perKmRate);
    }

    private void saveRideRequest() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("customerRequest").child(userId);

        Map<String, Object> rideMap = new HashMap<>();
        rideMap.put("customerId", mCurrentRide.getCustomerId());
        rideMap.put("pickupLat", mCurrentRide.getPickupLat());
        rideMap.put("pickupLng", mCurrentRide.getPickupLng());
        rideMap.put("pickupAddress", mCurrentRide.getPickupAddress());
        rideMap.put("destLat", mCurrentRide.getDestLat());
        rideMap.put("destLng", mCurrentRide.getDestLng());
        rideMap.put("destinationAddress", mCurrentRide.getDestinationAddress());
        rideMap.put("vehicleType", mCurrentRide.getVehicleType());
        rideMap.put("fare", mCurrentRide.getFare());
        rideMap.put("status", mCurrentRide.getStatus());
        rideMap.put("timestamp", mCurrentRide.getTimestamp());

        ref.setValue(rideMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Ride request saved"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save ride request", e));

        requestBol = true;
        mRequest.setText(R.string.getting_driver);
    }

    private void setupRideTimeout() {
        cancelHandler.postDelayed(() -> {
            if (mCurrentRide != null && mCurrentRide.getDriverId() == null) {
                runOnUiThread(() -> {
                    if (mTimeout != null) mTimeout.setVisibility(View.VISIBLE);
                });
            }
        }, CANCEL_OPTION_MILLISECONDS);

        timeoutHandler.postDelayed(() -> {
            if (mCurrentRide != null && mCurrentRide.getDriverId() == null) {
                runOnUiThread(() -> {
                    cancelRide();
                    showNoDriversDialog();
                });
            }
        }, TIMEOUT_MILLISECONDS);
    }

    private void showNoDriversDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.no_drivers_around)
                .setMessage(R.string.no_driver_found)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void requestListener() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference rideRef = FirebaseDatabase.getInstance()
                .getReference("customerRequest").child(userId);

        rideStatusListener = rideRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String status = snapshot.child("status").getValue(String.class);
                String driverId = snapshot.child("driverId").getValue(String.class);

                if ("cancelled".equals(status) || "completed".equals(status)) {
                    cleanupRide();
                    return;
                }

                if (driverId != null && mCurrentRide != null && mCurrentRide.getDriverId() == null) {
                    mCurrentRide.setDriverId(driverId);
                    cancelHandler.removeCallbacksAndMessages(null);
                    timeoutHandler.removeCallbacksAndMessages(null);
                    getDriverInfo(driverId);
                    getDriverLocation(driverId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Request listener cancelled: " + error.getMessage());
            }
        });
    }

    private void getDriverInfo(String driverId) {
        DatabaseReference driverRef = FirebaseDatabase.getInstance()
                .getReference().child("Users").child("Drivers").child(driverId);

        driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String name = snapshot.child("name").getValue(String.class);
                String car = snapshot.child("car").getValue(String.class);
                String phone = snapshot.child("phone").getValue(String.class);
                String image = snapshot.child("profileImageUrl").getValue(String.class);
                Double rating = snapshot.child("rating").getValue(Double.class);

                if (mCurrentRide != null) {
                    mCurrentRide.setDriverPhone(phone);
                }

                if (mDriverName != null) {
                    mDriverName.setText(name != null ? name : "Driver");
                }
                if (mDriverCar != null) {
                    mDriverCar.setText(car != null ? car : "Vehicle info");
                }
                if (mDriverLicense != null) {
                    mDriverLicense.setText(phone != null ? phone : "");
                }
                if (mRatingText != null) {
                    mRatingText.setText(String.format(Locale.getDefault(), "%.1f",
                            rating != null ? rating : 5.0));
                }

                if (image != null && !image.equals("default") && !image.isEmpty() && mDriverProfileImage != null) {
                    Glide.with(CustomerMapActivity.this)
                            .load(image)
                            .apply(RequestOptions.circleCropTransform())
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .into(mDriverProfileImage);
                }

                showBottomSheet(4);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error getting driver info: " + error.getMessage());
            }
        });
    }

    private void getDriverLocation(String driverId) {
        driverLocationRef = FirebaseDatabase.getInstance()
                .getReference().child("driversWorking").child(driverId).child("l");

        driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || !requestBol || mMap == null || pickupLatLng == null) return;

                List<Object> location = (List<Object>) snapshot.getValue();
                if (location == null || location.size() < 2) return;

                try {
                    double lat = Double.parseDouble(location.get(0).toString());
                    double lng = Double.parseDouble(location.get(1).toString());
                    LatLng driverLatLng = new LatLng(lat, lng);

                    updateDriverMarkerOnMap(driverLatLng);
                    updateDriverDistance(driverLatLng);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing driver location", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Driver location cancelled: " + error.getMessage());
            }
        });
    }

    private void updateDriverMarkerOnMap(LatLng driverLatLng) {
        if (mMap == null) return;

        if (mDriverMarker != null) mDriverMarker.remove();

        mDriverMarker = mMap.addMarker(new MarkerOptions()
                .position(driverLatLng)
                .title("Your Driver")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car_top)));
    }

    private void updateDriverDistance(LatLng driverLatLng) {
        if (pickupLatLng == null || mRequest == null) return;

        float[] results = new float[1];
        Location.distanceBetween(
                pickupLatLng.latitude, pickupLatLng.longitude,
                driverLatLng.latitude, driverLatLng.longitude,
                results);

        float distance = results[0];
        mRequest.setText(distance < 100 ? R.string.driver_here : R.string.driver_found);
    }

    private void cancelRide() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("customerRequest").child(userId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "cancelled");
        updates.put("cancelledBy", "customer");
        updates.put("cancelledTime", System.currentTimeMillis());

        ref.updateChildren(updates);
        cleanupRide();
    }

    private void cleanupRide() {
        requestBol = false;
        driverFound = false;

        cancelHandler.removeCallbacksAndMessages(null);
        timeoutHandler.removeCallbacksAndMessages(null);

        if (driverLocationRefListener != null && driverLocationRef != null) {
            driverLocationRef.removeEventListener(driverLocationRefListener);
            driverLocationRefListener = null;
        }

        if (rideStatusListener != null) {
            FirebaseDatabase.getInstance().getReference("customerRequest")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .removeEventListener(rideStatusListener);
            rideStatusListener = null;
        }

        // Remove from GeoFire
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        GeoFire geoFire = new GeoFire(FirebaseDatabase.getInstance()
                .getReference("customerRequest"));
        geoFire.removeLocation(userId);

        resetUI();
    }

    private void resetUI() {
        if (pickupMarker != null) pickupMarker.remove();
        if (destinationMarker != null) destinationMarker.remove();
        if (mDraggableDestinationMarker != null) mDraggableDestinationMarker.remove();
        if (mDriverMarker != null) mDriverMarker.remove();

        clearPolylines();

        if (mRequest != null) {
            mRequest.setText(R.string.call_uber);
        }
        if (autocompleteFragmentTo != null) {
            autocompleteFragmentTo.setText("");
            autocompleteFragmentTo.setHint(R.string.to);
        }
        if (autocompleteFragmentFrom != null) {
            autocompleteFragmentFrom.setText("");
            autocompleteFragmentFrom.setHint(R.string.from);
        }
        if (mCurrentLocation != null) {
            mCurrentLocation.setImageResource(R.drawable.ic_location_on_grey_24dp);
        }

        pickupLatLng = null;
        destinationLatLng = null;
        mCurrentRide = new RideRequest();

        showBottomSheet(1);
        getDriversAround();

        // Re-set current location as pickup
        setCurrentLocationAsPickup();
    }

    private void clearPolylines() {
        for (Polyline polyline : polylines) {
            if (polyline != null) polyline.remove();
        }
        polylines.clear();
    }

    private void calculateRoute() {
        if (pickupLatLng == null || destinationLatLng == null || mRouteHelper == null) return;
        mRouteHelper.getRoute(pickupLatLng, destinationLatLng);
    }

    @Override
    public void onRouteSuccess(ArrayList<LatLng> path, double distance, int duration) {
        if (mMap == null) return;

        clearPolylines();

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(path)
                .width(12)
                .color(ContextCompat.getColor(this, R.color.colorPrimary))
                .geodesic(true);

        polylines.add(mMap.addPolyline(polylineOptions));

        routeData = new ArrayList<>();
        routeData.add(distance);
        routeData.add((double) duration);

        if (mAdapter != null) {
            mAdapter.setRouteData(routeData);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onRouteFailure(String error) {
        Log.e(TAG, "Route error: " + error);
        if (drawer != null) {
            Snackbar.make(drawer, "Route error: " + error, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void showBottomSheet(int status) {
        if (mContainer == null) return;

        int animationRes;

        if (status > bottomSheetStatus) {
            animationRes = R.anim.slide_up;
        } else {
            animationRes = R.anim.slide_down;
        }

        bottomSheetStatus = status;

        Animation animation =
                AnimationUtils.loadAnimation(this, animationRes);

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                updateBottomSheetContent();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        mContainer.startAnimation(animation);
    }

    private void updateBottomSheetContent() {
        if (mLocation != null) {
            mLocation.setVisibility(bottomSheetStatus == 1 ? View.VISIBLE : View.GONE);
        }
        if (mRadioLayout != null) {
            mRadioLayout.setVisibility(bottomSheetStatus == 2 ? View.VISIBLE : View.GONE);
        }
        if (mLooking != null) {
            mLooking.setVisibility(bottomSheetStatus == 3 ? View.VISIBLE : View.GONE);
        }
        if (mDriverInfo != null) {
            mDriverInfo.setVisibility(bottomSheetStatus == 4 ? View.VISIBLE : View.GONE);
        }
        if (mTimeout != null) {
            mTimeout.setVisibility(bottomSheetStatus == 3 &&
                    (mCurrentRide == null || mCurrentRide.getDriverId() == null) ?
                    View.VISIBLE : View.GONE);
        }
    }

    private void callDriver() {
        if (mCurrentRide == null || mCurrentRide.getDriverPhone() == null) return;

        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + mCurrentRide.getDriverPhone()));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            startActivity(intent);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE}, 101);
        }
    }

    private void loadUserProfile() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference().child("Users").child("Customers").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || navigationView == null) return;

                View header = navigationView.getHeaderView(0);
                if (header == null) return;

                TextView usernameDrawer = header.findViewById(R.id.usernameDrawer);
                ImageView imageViewDrawer = header.findViewById(R.id.imageViewDrawer);

                String name = snapshot.child("name").getValue(String.class);
                String image = snapshot.child("profileImageUrl").getValue(String.class);

                if (usernameDrawer != null) {
                    usernameDrawer.setText(name != null ? name : "User");
                }

                if (image != null && !image.equals("default") && !image.isEmpty() && imageViewDrawer != null) {
                    Glide.with(CustomerMapActivity.this)
                            .load(image)
                            .apply(RequestOptions.circleCropTransform())
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .into(imageViewDrawer);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkForActiveRide() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference rideRef = FirebaseDatabase.getInstance()
                .getReference("customerRequest").child(userId);

        rideRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String status = snapshot.child("status").getValue(String.class);
                String driverId = snapshot.child("driverId").getValue(String.class);

                if ("pending".equals(status) || "accepted".equals(status) || "started".equals(status)) {
                    // Restore active ride
                    if (mCurrentRide != null) {
                        mCurrentRide.setDriverId(driverId);

                        Double pickupLat = snapshot.child("pickupLat").getValue(Double.class);
                        Double pickupLng = snapshot.child("pickupLng").getValue(Double.class);
                        Double destLat = snapshot.child("destLat").getValue(Double.class);
                        Double destLng = snapshot.child("destLng").getValue(Double.class);
                        String pickupAddr = snapshot.child("pickupAddress").getValue(String.class);
                        String destAddr = snapshot.child("destinationAddress").getValue(String.class);

                        if (pickupLat != null && pickupLng != null) {
                            pickupLatLng = new LatLng(pickupLat, pickupLng);
                            pickupAddress = pickupAddr;
                            updatePickupMarker();
                            if (autocompleteFragmentFrom != null) {
                                autocompleteFragmentFrom.setText(pickupAddr);
                            }
                        }

                        if (destLat != null && destLng != null) {
                            destinationLatLng = new LatLng(destLat, destLng);
                            destinationAddress = destAddr;
                            if (autocompleteFragmentTo != null) {
                                autocompleteFragmentTo.setText(destAddr);
                            }
                            if (mMap != null) {
                                destinationMarker = mMap.addMarker(new MarkerOptions()
                                        .position(destinationLatLng)
                                        .title("Destination")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                            }
                        }
                    }

                    requestBol = true;

                    if (driverId != null) {
                        getDriverInfo(driverId);
                        getDriverLocation(driverId);
                        showBottomSheet(4);
                    } else {
                        showBottomSheet(3);
                    }

                    requestListener();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void logout() {
        Log.d(TAG, "logout() called");

        if (requestBol) {
            Toast.makeText(this, "Cannot logout during active ride", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Log.d(TAG, "User confirmed logout");

                    // Show a progress message
                    Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show();

                    // Sign out from Firebase
                    FirebaseAuth.getInstance().signOut();

                    // Navigate to LauncherActivity
                    Intent intent = new Intent(CustomerMapActivity.this, LauncherActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            LatLng latLng = place.getLatLng();
            String address = place.getName();

            if (requestCode == AUTOCOMPLETE_REQUEST_CODE_TO) {
                destinationLatLng = latLng;
                destinationAddress = address;
                if (autocompleteFragmentTo != null) {
                    autocompleteFragmentTo.setText(address);
                }

                // Remove draggable marker if exists
                if (mDraggableDestinationMarker != null) {
                    mDraggableDestinationMarker.remove();
                    mDraggableDestinationMarker = null;
                }

                if (destinationMarker != null && mMap != null) {
                    destinationMarker.remove();
                }
                if (mMap != null && destinationLatLng != null) {
                    destinationMarker = mMap.addMarker(new MarkerOptions()
                            .position(destinationLatLng)
                            .title("Destination")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                }

                if (pickupLatLng != null) {
                    calculateRoute();
                    showBottomSheet(2);
                }
            } else if (requestCode == AUTOCOMPLETE_REQUEST_CODE_FROM) {
                pickupLatLng = latLng;
                pickupAddress = address;
                if (autocompleteFragmentFrom != null) {
                    autocompleteFragmentFrom.setText(address);
                }

                if (pickupMarker != null && mMap != null) {
                    pickupMarker.remove();
                }
                if (mMap != null && pickupLatLng != null) {
                    pickupMarker = mMap.addMarker(new MarkerOptions()
                            .position(pickupLatLng)
                            .title("Pickup")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                }

                if (destinationLatLng != null) {
                    calculateRoute();
                    showBottomSheet(2);
                }
            }
        } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
            Status status = Autocomplete.getStatusFromIntent(data);
            Log.e(TAG, "Places Error: " + (status != null ? status.getStatusMessage() : "unknown"));
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.history) {
            startActivity(new Intent(this, HistoryActivity.class)
                    .putExtra("userType", "Customers"));
        }
        else if (id == R.id.payment) {
            startActivity(new Intent(this, AddPaymentActivity.class));
        }
        else if (id == R.id.settings) {
            startActivity(new Intent(this, CustomerSettingsActivity.class));
        }
        else if (id == R.id.help) {
            Toast.makeText(this, "Help section coming soon", Toast.LENGTH_SHORT).show();
        }
        else if (id == R.id.logout) {
            logout();
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (bottomSheetStatus > 1) {
            if (requestBol) {
                new AlertDialog.Builder(this)
                        .setTitle("Cancel Ride")
                        .setMessage("Are you sure you want to cancel this ride?")
                        .setPositiveButton("Yes", (dialog, which) -> cancelRide())
                        .setNegativeButton("No", null)
                        .show();
            } else {
                showBottomSheet(1);
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
                setCurrentLocationAsPickup();
            } else {
                Toast.makeText(this, "Location permission is required to use this app", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                callDriver();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (driverLocationRefListener != null && driverLocationRef != null) {
            driverLocationRef.removeEventListener(driverLocationRefListener);
        }

        if (rideStatusListener != null) {
            FirebaseDatabase.getInstance().getReference("customerRequest")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .removeEventListener(rideStatusListener);
        }

        if (mRouteHelper != null) {
            mRouteHelper.shutdown();
        }

        if (mLocationCallback != null) {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.removeLocationUpdates(mLocationCallback);
        }

        Log.d(TAG, "onDestroy completed");
    }
}