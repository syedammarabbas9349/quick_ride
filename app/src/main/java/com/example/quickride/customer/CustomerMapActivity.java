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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import com.example.quickride.R;
import com.example.quickride.adapters.TypeAdapter;
import com.example.quickride.auth.LauncherActivity;
import com.example.quickride.history.HistoryActivity;
import com.example.quickride.models.RideRequest;
import com.example.quickride.models.ServiceType;
import com.example.quickride.models.SharedPassenger;
import com.example.quickride.payment.AddPaymentActivity;
import com.example.quickride.payment.PaymentActivity;
import com.example.quickride.utils.PaymentUtils;
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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import com.google.android.gms.maps.model.BitmapDescriptor;
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
import com.google.firebase.auth.FirebaseUser;
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
    private static final int GPS_SETTINGS_REQUEST_CODE = 1001;

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
    private androidx.appcompat.widget.SwitchCompat switchTrustedMode;

    // Data
    private RideRequest mCurrentRide;
    private LocationHelper mLocationHelper;
    private RouteHelper mRouteHelper;
    private NotificationHelper mNotificationHelper;
    private com.example.quickride.utils.VoiceHelper mVoiceHelper;


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
    private String mCustomerName = "Customer";
    private String mCustomerPhone = "";
    private String mCustomerProfileImage = "default";

    // Driver Search State
    private GeoQuery searchGeoQuery;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private int currentSearchRadius = 2;
    private boolean driverSearchActive = false;

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
        switchTrustedMode = findViewById(R.id.switchTrustedMode);
        switchTrustedMode = findViewById(R.id.switchTrustedMode);

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
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mNotificationHelper.initialize("Customers", userId);
        
        mVoiceHelper = new com.example.quickride.utils.VoiceHelper(this);

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
                (type, position, sharingEnabled) -> {  // ✅ 3 parameters
                    Log.d(TAG,"Vehicle selected: " + type.getName() + ", Sharing: " + sharingEnabled);
                });

        mRecyclerView.setAdapter(mAdapter);
    }

    private ArrayList<ServiceType> getTypeList() {
        ArrayList<ServiceType> types = new ArrayList<>();

        ServiceType bike = new ServiceType("bike", "Bike", "bike", 10.0, 1, R.drawable.ic_bike);
        bike.setBaseFare(30.0);
        bike.setMinimumFare(50.0);
        types.add(bike);

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
            mLocationHelper.checkLocationSettings(this, GPS_SETTINGS_REQUEST_CODE);
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

                // ✅ Set current location in LocationHelper
                mLocationHelper.setCurrentLocation(latLng);

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

            // ✅ Try to get last known location immediately
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mLocationHelper.setCurrentLocation(latLng);
                        if (!getDriversAroundStarted) {
                            getDriversAround();
                        }
                    }
                });
            }
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
        LatLng current = mLocationHelper.getCurrentLocation();

        if (current == null) {
            Log.e(TAG, "❌ Cannot search: current location is null");

            // Try to get location one more time
            if (mLocationHelper.hasLocationPermission()) {
                FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                        if (location != null) {
                            LatLng newLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mLocationHelper.setCurrentLocation(newLocation);
                            currentSearchRadius = 2;
                            driverSearchActive = true;
                            performDriverSearch(newLocation);
                        } else {
                            Log.e(TAG, "❌ Still no location after retry");
                        }
                    });
                    return;
                }
            }
            return;
        }

        getDriversAroundStarted = true;
        currentSearchRadius = 2;
        driverSearchActive = true;
        performDriverSearch(current);
    }

    private void performDriverSearch(LatLng current) {
        if (searchGeoQuery != null) {
            searchGeoQuery.removeAllListeners();
        }
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }

        updateSearchUI("Searching in " + currentSearchRadius + "km radius...");

        Log.d(TAG, "======================================");
        Log.d(TAG, "🔍 SEARCHING FOR DRIVERS");
        Log.d(TAG, "📍 Customer location: " + current.latitude + ", " + current.longitude);
        Log.d(TAG, "📏 Search radius: " + currentSearchRadius + " km");
        Log.d(TAG, "======================================");

        DatabaseReference driversLocation = FirebaseDatabase.getInstance()
                .getReference().child("driversWorking");

        GeoFire geoFire = new GeoFire(driversLocation);
        searchGeoQuery = geoFire.queryAtLocation(
                new GeoLocation(current.latitude, current.longitude), currentSearchRadius);

        searchGeoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                Log.d(TAG, "✅ DRIVER FOUND: " + key);
                Log.d(TAG, "   Location: " + location.latitude + ", " + location.longitude);
                addDriverMarker(key, location);
            }

            @Override
            public void onKeyExited(String key) {
                Log.d(TAG, "🚫 Driver exited: " + key);
                removeDriverMarker(key);
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                Log.d(TAG, "🔄 Driver moved: " + key);
                updateDriverMarker(key, location);
            }

            @Override
            public void onGeoQueryReady() {
                Log.d(TAG, "✅ GeoQuery complete - waiting 5 seconds to check if drivers found");
                searchRunnable = () -> {
                    if (driverMarkers.isEmpty()) {
                        expandSearchRadius(current);
                    } else {
                        updateSearchUI("Drivers found nearby.");
                        driverSearchActive = false;
                    }
                };
                searchHandler.postDelayed(searchRunnable, 5000);
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e(TAG, "❌ GeoQuery error: " + error.getMessage());
                updateSearchUI("Error searching for drivers");
                driverSearchActive = false;
            }
        });
    }

    private void expandSearchRadius(LatLng current) {
        if (currentSearchRadius == 2) {
            currentSearchRadius = 5;
            performDriverSearch(current);
        } else if (currentSearchRadius == 5) {
            currentSearchRadius = 10;
            performDriverSearch(current);
        } else {
            // Termination condition: No drivers found after 10km
            if (searchGeoQuery != null) {
                searchGeoQuery.removeAllListeners();
            }
            driverSearchActive = false;
            updateSearchUI("No drivers found nearby");
            showNoDriversNearbySnackbar();
            
            // Stop getting driver loop
            if (requestBol && mRequest != null && mRequest.getText().toString().equals(getString(R.string.getting_driver))) {
                cancelRide();
            }
        }
    }

    private void updateSearchUI(String message) {
        if (requestBol && mRequest != null && mRequest.getText().toString().equals(getString(R.string.getting_driver))) {
            mRequest.setText(message);
        }
        
        // As a progress update, show a toast or log if not in active request bol but UI update is requested based on the prompt
        Log.d(TAG, "Search UI Update: " + message);
    }

    private void showNoDriversNearbySnackbar() {
        if (drawer != null) {
            Snackbar.make(drawer, "No Drivers Nearby in 10km radius", Snackbar.LENGTH_LONG)
                .setAction("Retry", v -> {
                    currentSearchRadius = 2;
                    driverSearchActive = true;
                    performDriverSearch(mLocationHelper.getCurrentLocation());
                }).show();
        } else {
            Toast.makeText(this, "No Drivers Nearby in 10km radius", Toast.LENGTH_LONG).show();
        }
    }

    private void addDriverMarker(String driverId, GeoLocation location) {
        if (mCurrentRide == null || mCurrentRide.getDriverId() != null || mMap == null) return;

        // Check if marker already exists to ensure uniqueness
        for (Marker marker : driverMarkers) {
            if (marker.getTag() != null && marker.getTag().equals(driverId)) {
                return;
            }
        }

        LatLng driverLatLng = new LatLng(location.latitude, location.longitude);
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(driverLatLng)
                .icon(getBitmapDescriptor(R.drawable.ic_car_top))
                .title(driverId));

        marker.setTag(driverId);
        driverMarkers.add(marker);
    }

    private BitmapDescriptor getBitmapDescriptor(int id) {
        Drawable vectorDrawable = ContextCompat.getDrawable(this, id);
        if (vectorDrawable == null) return BitmapDescriptorFactory.defaultMarker();
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
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

        // Show Payment Selection Dialog first
        PaymentUtils.showPaymentSelectionDialog(this, (method, accountNumber) -> {
            if (mCurrentRide != null) {
                mCurrentRide.setPaymentMethod(method);
                mCurrentRide.setTransactionId(accountNumber);
            }
            proceedToSearchDriver();
        });
    }

    private void proceedToSearchDriver() {
        if (switchTrustedMode != null && switchTrustedMode.isChecked()) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference trustedRef = FirebaseDatabase.getInstance().getReference()
                    .child("Users").child("Customers").child(userId).child("TrustedDrivers");
            
            trustedRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String selectedTrustedDriverId = null;
                    if (snapshot.exists()) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String driverId = ds.getKey();
                            if (ds.getValue(Boolean.class) != null && ds.getValue(Boolean.class)) {
                                // Check if this driver is nearby (in driverMarkers)
                                for (Marker marker : driverMarkers) {
                                    if (marker.getTag() != null && marker.getTag().equals(driverId)) {
                                        selectedTrustedDriverId = driverId;
                                        break;
                                    }
                                }
                            }
                            if (selectedTrustedDriverId != null) break;
                        }
                    }
                    if (selectedTrustedDriverId != null) {
                        Toast.makeText(CustomerMapActivity.this, "Requesting Trusted Driver first...", Toast.LENGTH_SHORT).show();
                        executeRideRequest(selectedTrustedDriverId);
                    } else {
                        Toast.makeText(CustomerMapActivity.this, "No Trusted Drivers nearby. Broadcasting to all.", Toast.LENGTH_SHORT).show();
                        executeRideRequest(null);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    executeRideRequest(null);
                }
            });
        } else {
            executeRideRequest(null);
        }
    }

    private void executeRideRequest(String targetDriverId) {
        showBottomSheet(3);

        // Get sharing status from adapter
        boolean sharingEnabled = mAdapter.isSharingEnabled();
        ServiceType selectedType = mAdapter.getSelectedItem();

        // Calculate fares
        double originalFare = calculateFare();
        double finalFare = sharingEnabled ?
                originalFare * (1 - selectedType.getSharingDiscount()) : originalFare;

        // Create shareRideId if sharing enabled
        String shareRideId = sharingEnabled ?
                FirebaseDatabase.getInstance().getReference().child("shared_rides").push().getKey() : null;

        mCurrentRide.setCustomerId(FirebaseAuth.getInstance().getCurrentUser().getUid());
        mCurrentRide.setPickupLat(pickupLatLng.latitude);
        mCurrentRide.setPickupLng(pickupLatLng.longitude);
        mCurrentRide.setPickupAddress(pickupAddress);
        mCurrentRide.setDestLat(destinationLatLng.latitude);
        mCurrentRide.setDestLng(destinationLatLng.longitude);
        mCurrentRide.setDestinationAddress(destinationAddress);
        mCurrentRide.setVehicleType(selectedType.getVehicleType());
        mCurrentRide.setOriginalFare(originalFare);
        mCurrentRide.setFare(finalFare);
        mCurrentRide.setSharingEnabled(sharingEnabled);
        mCurrentRide.setSharingDiscount(selectedType.getSharingDiscount());
        mCurrentRide.setShareRideId(shareRideId);
        mCurrentRide.setMaxPassengers(selectedType.getMaxSharedPassengers());
        mCurrentRide.setCurrentPassengers(1);
        mCurrentRide.setShareStatus(sharingEnabled ? "sharing" : "solo");
        mCurrentRide.setStatus("pending");
        mCurrentRide.setTimestamp(System.currentTimeMillis());
        if (targetDriverId != null) {
            mCurrentRide.setTrustedDriverOnly(targetDriverId);
        }
        
        // Set distance from route calculation
        if (routeData != null && !routeData.isEmpty()) {
            mCurrentRide.setDistance(routeData.get(0));
        }

        // Create passenger info if sharing
        if (sharingEnabled) {
            List<SharedPassenger> passengers = new ArrayList<>();
            SharedPassenger currentPassenger = new SharedPassenger(
                    mCurrentRide.getCustomerId(),
                    getCurrentCustomerName(),
                    getCurrentCustomerPhone(),
                    getCurrentCustomerImageUrl(),
                    true
            );
            currentPassenger.setPickupLat(pickupLatLng.latitude);
            currentPassenger.setPickupLng(pickupLatLng.longitude);
            currentPassenger.setPickupAddress(pickupAddress);
            currentPassenger.setDropoffLat(destinationLatLng.latitude);
            currentPassenger.setDropoffLng(destinationLatLng.longitude);
            currentPassenger.setDropoffAddress(destinationAddress);
            currentPassenger.setFareShare(finalFare);
            currentPassenger.setStatus("pending");

            passengers.add(currentPassenger);
            mCurrentRide.setPassengers(passengers);
        }

        saveRideRequest();
        setupRideTimeout();
        requestListener();
    }

    // Helper methods to get customer info
    private String getCurrentCustomerName() {
        return mCustomerName != null ? mCustomerName : "Customer";
    }

    private String getCurrentCustomerPhone() {
        return mCustomerPhone != null ? mCustomerPhone : "";
    }

    private String getCurrentCustomerImageUrl() {
        return mCustomerProfileImage != null ? mCustomerProfileImage : "default";
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
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);

        // Use GeoFire setLocation to ensure geohash 'g' and location 'l' are created correctly
        geoFire.setLocation(userId, new GeoLocation(pickupLatLng.latitude, pickupLatLng.longitude),
                (key, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ GeoFire error: " + error.getMessage());
                        return;
                    }

                    DatabaseReference rideRef = ref.child(userId);
                    Map<String, Object> rideMap = new HashMap<>();
                    rideMap.put("customerId", mCurrentRide.getCustomerId());
                    rideMap.put("customerName", getCurrentCustomerName());
                    rideMap.put("customerPhone", getCurrentCustomerPhone());
                    rideMap.put("customerImageUrl", getCurrentCustomerImageUrl());
                    rideMap.put("pickupLat", mCurrentRide.getPickupLat());
                    rideMap.put("pickupLng", mCurrentRide.getPickupLng());
                    rideMap.put("pickupAddress", mCurrentRide.getPickupAddress());
                    rideMap.put("destLat", mCurrentRide.getDestLat());
                    rideMap.put("destLng", mCurrentRide.getDestLng());
                    rideMap.put("destinationAddress", mCurrentRide.getDestinationAddress());
                    rideMap.put("vehicleType", mCurrentRide.getVehicleType());
                    rideMap.put("fare", mCurrentRide.getFare());
                    rideMap.put("status", mCurrentRide.getStatus());
                    rideMap.put("sharingEnabled", mCurrentRide.isSharingEnabled());
                    rideMap.put("timestamp", mCurrentRide.getTimestamp());
                    rideMap.put("distance", mCurrentRide.getDistance());
                    rideMap.put("paymentMethod", mCurrentRide.getPaymentMethod() != null ? mCurrentRide.getPaymentMethod() : "cash");
                    if (mCurrentRide.getTransactionId() != null) {
                        rideMap.put("transactionId", mCurrentRide.getTransactionId());
                    }
                    if (mCurrentRide.getTrustedDriverOnly() != null) {
                        rideMap.put("trustedDriverOnly", mCurrentRide.getTrustedDriverOnly());
                    }

                    Log.d(TAG, "======================================");
                    Log.d(TAG, "📢 UPDATING RIDE REQUEST DATA");
                    Log.d(TAG, "   Path: " + rideRef.toString());
                    Log.d(TAG, "   Vehicle type: '" + mCurrentRide.getVehicleType() + "'");
                    Log.d(TAG, "   Status: " + mCurrentRide.getStatus());
                    Log.d(TAG, "======================================");

                    rideRef.updateChildren(rideMap)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ Ride request data updated successfully!");
                                // Broadcast notification to all drivers
                                mNotificationHelper.broadcastRideRequest(
                                        getCurrentCustomerName(),
                                        mCurrentRide.getPickupAddress(),
                                        userId
                                );
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to update ride request data", e));

                });

        requestBol = true;
        mRequest.setText(R.string.getting_driver);
    }

    private void setupRideTimeout() {
        if (mCurrentRide != null && mCurrentRide.getTrustedDriverOnly() != null) {
            // Wait 30 seconds for trusted driver, then fallback to normal
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (mCurrentRide != null && mCurrentRide.getDriverId() == null && requestBol) {
                    // Fallback to normal
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference rideRef = FirebaseDatabase.getInstance().getReference("customerRequest").child(userId);
                    rideRef.child("trustedDriverOnly").removeValue();
                    mCurrentRide.setTrustedDriverOnly(null);
                    Toast.makeText(CustomerMapActivity.this, "Trusted driver didn't accept. Broadcasting to all.", Toast.LENGTH_LONG).show();
                }
            }, 30000);
        }

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

                if ("cancelled".equals(status)) {
                    cleanupRide();
                    return;
                }

                if ("completed".equals(status)) {
                    if (mVoiceHelper != null) {
                        mVoiceHelper.speak("Your ride is complete. Thank you for riding with Quick Ride.");
                    }
                    // Show fare summary to rider before resetting UI
                    showRideFinishedDialog();
                    return;
                }


                if ("declined".equals(status)) {
                    Toast.makeText(CustomerMapActivity.this, "Driver declined your request.", Toast.LENGTH_LONG).show();
                    new androidx.appcompat.app.AlertDialog.Builder(CustomerMapActivity.this)
                        .setTitle("Ride Declined")
                        .setMessage("A driver has declined your ride request. Please try requesting again.")
                        .setPositiveButton("OK", null)
                        .show();
                    cleanupRide();
                    return;
                }
                
                if ("arrived".equals(status)) {
                    mRequest.setText("Driver has arrived!");
                    Toast.makeText(CustomerMapActivity.this, "Your driver is waiting outside.", Toast.LENGTH_SHORT).show();
                    if (mVoiceHelper != null) {
                        mVoiceHelper.speak("Your driver has arrived and is waiting outside.");
                    }
                } else if ("started".equals(status)) {

                    mRequest.setText("Ride in progress");
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
                .icon(getBitmapDescriptor(R.drawable.ic_car_top)));
    }

    private void updateDriverDistance(LatLng driverLatLng) {
        if (pickupLatLng == null || mRequest == null) return;

        float[] results = new float[1];
        Location.distanceBetween(
                pickupLatLng.latitude, pickupLatLng.longitude,
                driverLatLng.latitude, driverLatLng.longitude,
                results);

        float distance = results[0];
        
        String currentText = mRequest.getText().toString();
        if (!"Driver has arrived!".equals(currentText) && !"Ride in progress".equals(currentText)) {
            mRequest.setText(distance < 100 ? R.string.driver_here : R.string.driver_found);
        }
    }

    /**
     * Called when the driver marks the ride as 'completed'.
     * Shows a fare-summary dialog before resetting the UI.
     */
    private void showRideFinishedDialog() {
        double fare = (mCurrentRide != null) ? mCurrentRide.getFare() : 0;
        double dist = 0;
        if (mCurrentRide != null && routeData != null && !routeData.isEmpty()) {
            dist = routeData.get(0);
        }

        // Build display strings
        String fareStr = fare > 0
                ? String.format(Locale.getDefault(), "Rs. %.0f", fare)
                : "Check with your driver";
        String distStr = dist > 0
                ? String.format(Locale.getDefault(), "%.1f km", dist)
                : "N/A";

        String paymentMethod = (mCurrentRide != null && mCurrentRide.getPaymentMethod() != null)
                ? mCurrentRide.getPaymentMethod() : "Cash";

        new AlertDialog.Builder(this)
                .setTitle("🏁 Ride Finished!")
                .setMessage("Your trip has been completed.\n\n"
                        + "Distance:        " + distStr + "\n"
                        + "Total Fare:      " + fareStr + "\n"
                        + "Payment:         " + paymentMethod + "\n\n"
                        + "Thank you for riding with QuickRide!")
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    cleanupRide();
                })
                .setCancelable(false)
                .show();
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
        
        // Also notify the driver by updating ride_info
        DatabaseReference rideInfoRef = FirebaseDatabase.getInstance()
                .getReference("ride_info").child(userId);
        rideInfoRef.updateChildren(updates);
        
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
                if (!snapshot.exists()) return;

                mCustomerName = snapshot.child("name").getValue(String.class);
                mCustomerPhone = snapshot.child("phone").getValue(String.class);
                mCustomerProfileImage = snapshot.child("profileImageUrl").getValue(String.class);

                if (navigationView == null) return;

                View header = navigationView.getHeaderView(0);
                if (header == null) return;

                TextView usernameDrawer = header.findViewById(R.id.usernameDrawer);
                ImageView imageViewDrawer = header.findViewById(R.id.imageViewDrawer);

                if (usernameDrawer != null) {
                    usernameDrawer.setText(mCustomerName != null ? mCustomerName : "User");
                }

                if (mCustomerProfileImage != null && !mCustomerProfileImage.equals("default") && !mCustomerProfileImage.isEmpty() && imageViewDrawer != null) {
                    Glide.with(CustomerMapActivity.this)
                            .load(mCustomerProfileImage)
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

        if (requestCode == GPS_SETTINGS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // GPS enabled, refresh location
                startLocationUpdates();
                setCurrentLocationAsPickup();
            } else {
                Toast.makeText(this, "Please turn on location to auto-detect your pickup point", Toast.LENGTH_LONG).show();
            }
            return;
        }

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
        else if (id == R.id.fixed_routes) {

            startActivity(new Intent(this, FixedRoutesSearchActivity.class));
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
        if (mVoiceHelper != null) {
            mVoiceHelper.shutdown();
        }

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
        super.onDestroy();
    }
}