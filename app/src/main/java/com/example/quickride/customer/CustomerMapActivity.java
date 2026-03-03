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
    private LinearLayout mDriverInfo, mRadioLayout, mLocation, mLooking, mTimeout;
    private ImageView mDriverProfileImage, mDrawerButton;
    private TextView mDriverName, mDriverCar, mDriverLicense, mRatingText;
    private TextView autocompleteFragmentTo, autocompleteFragmentFrom;
    private Button mRequest;
    private FloatingActionButton mCallDriver, mCancel, mCancelTimeout, mCurrentLocation;
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
    private Marker mDriverMarker, pickupMarker, destinationMarker;
    private List<Polyline> polylines = new ArrayList<>();
    private ArrayList<Double> routeData;

    // State
    private boolean requestBol = false;
    private boolean driverFound = false;
    private boolean zoomUpdated = false;
    private boolean getDriversAroundStarted = false;
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
        setContentView(R.layout.activity_customer_map);

        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        initializeHelpers();
        setupPlaces();
        setupRecyclerView();
        setupLocation();
        checkForActiveRide();
        setupMap();
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
        mCallDriver = findViewById(R.id.phone);
        mCancel = findViewById(R.id.cancel);
        mCancelTimeout = findViewById(R.id.cancel_looking);
        mCurrentLocation = findViewById(R.id.current_location);
        mRecyclerView = findViewById(R.id.recyclerView);

        Button mLogout = findViewById(R.id.logout);
        mLogout.setOnClickListener(v -> logout());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mDrawerButton.setOnClickListener(v -> drawer.openDrawer(Gravity.LEFT));
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
            if (requestBol) return;
            openPlaceAutocomplete(AUTOCOMPLETE_REQUEST_CODE_TO);
        });

        autocompleteFragmentFrom.setOnClickListener(v -> {
            if (requestBol) return;
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
        typeArrayList = getTypeList();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mAdapter = new TypeAdapter(typeArrayList, this, routeData, (type, position) -> {
            // Handle type selection if needed
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
            for (Location location : locationResult.getLocations()) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                if (!zoomUpdated) {
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
        if (mLocationHelper.getCurrentLocation() == null) return;

        pickupLatLng = mLocationHelper.getCurrentLocation();
        pickupAddress = "Current Location";

        autocompleteFragmentFrom.setText(pickupAddress);
        mCurrentLocation.setImageResource(R.drawable.ic_location_on_primary_24dp);

        updatePickupMarker();
        fetchAddressFromLocation(pickupLatLng);

        if (destinationLatLng != null) {
            calculateRoute();
            showBottomSheet(2);
        }
    }

    private void fetchAddressFromLocation(LatLng latLng) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(
                    latLng.latitude, latLng.longitude, 1);

            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                pickupAddress = address.getAddressLine(0);
                autocompleteFragmentFrom.setText(pickupAddress);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updatePickupMarker() {
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

        if (mLocationHelper.hasLocationPermission()) {
            mMap.setMyLocationEnabled(true);
            startLocationUpdates();
        }
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
            public void onGeoQueryError(DatabaseError error) {}
        });
    }

    private void addDriverMarker(String driverId, GeoLocation location) {
        if (mCurrentRide.getDriverId() != null) return;

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

        ref.setValue(rideMap);
        requestBol = true;
        mRequest.setText(R.string.getting_driver);
    }

    private void setupRideTimeout() {
        cancelHandler.postDelayed(() -> {
            if (mCurrentRide.getDriverId() == null) {
                runOnUiThread(() -> mTimeout.setVisibility(View.VISIBLE));
            }
        }, CANCEL_OPTION_MILLISECONDS);

        timeoutHandler.postDelayed(() -> {
            if (mCurrentRide.getDriverId() == null) {
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

                if (driverId != null && mCurrentRide.getDriverId() == null) {
                    mCurrentRide.setDriverId(driverId);
                    cancelHandler.removeCallbacksAndMessages(null);
                    timeoutHandler.removeCallbacksAndMessages(null);
                    getDriverInfo(driverId);
                    getDriverLocation(driverId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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

                mDriverName.setText(name != null ? name : "Driver");
                mDriverCar.setText(car != null ? car : "Vehicle info");
                mDriverLicense.setText(phone != null ? phone : "");
                mRatingText.setText(String.format(Locale.getDefault(), "%.1f",
                        rating != null ? rating : 5.0));

                if (image != null && !image.equals("default") && !image.isEmpty()) {
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
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void getDriverLocation(String driverId) {
        driverLocationRef = FirebaseDatabase.getInstance()
                .getReference().child("driversWorking").child(driverId).child("l");

        driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || !requestBol) return;

                List<Object> location = (List<Object>) snapshot.getValue();
                if (location == null || location.size() < 2) return;

                double lat = Double.parseDouble(location.get(0).toString());
                double lng = Double.parseDouble(location.get(1).toString());
                LatLng driverLatLng = new LatLng(lat, lng);

                updateDriverMarkerOnMap(driverLatLng);
                updateDriverDistance(driverLatLng);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateDriverMarkerOnMap(LatLng driverLatLng) {
        if (mDriverMarker != null) mDriverMarker.remove();

        mDriverMarker = mMap.addMarker(new MarkerOptions()
                .position(driverLatLng)
                .title("Your Driver")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car_top)));
    }

    private void updateDriverDistance(LatLng driverLatLng) {
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
        }

        if (rideStatusListener != null) {
            FirebaseDatabase.getInstance().getReference("customerRequest")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .removeEventListener(rideStatusListener);
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
        if (mDriverMarker != null) mDriverMarker.remove();

        clearPolylines();

        mRequest.setText(R.string.call_uber);
        autocompleteFragmentTo.setText(R.string.to);
        autocompleteFragmentFrom.setText(R.string.from);
        mCurrentLocation.setImageResource(R.drawable.ic_location_on_grey_24dp);

        pickupLatLng = null;
        destinationLatLng = null;
        mCurrentRide = new RideRequest();

        showBottomSheet(1);
        getDriversAround();
    }

    private void clearPolylines() {
        for (Polyline polyline : polylines) {
            polyline.remove();
        }
        polylines.clear();
    }

    private void calculateRoute() {
        if (pickupLatLng == null || destinationLatLng == null) return;
        mRouteHelper.getRoute(pickupLatLng, destinationLatLng);
    }

    @Override
    public void onRouteSuccess(ArrayList<LatLng> path, double distance, int duration) {
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
        Log.e("Route Error", error);
        Snackbar.make(drawer, "Route error: " + error, Snackbar.LENGTH_SHORT).show();
    }

    private void showBottomSheet(int status) {
        bottomSheetStatus = status;
        int animationRes = status > bottomSheetStatus ? R.anim.slide_up : R.anim.slide_down;

        Animation animation = AnimationUtils.loadAnimation(this, animationRes);
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
        mLocation.setVisibility(bottomSheetStatus == 1 ? View.VISIBLE : View.GONE);
        mRadioLayout.setVisibility(bottomSheetStatus == 2 ? View.VISIBLE : View.GONE);
        mLooking.setVisibility(bottomSheetStatus == 3 ? View.VISIBLE : View.GONE);
        mDriverInfo.setVisibility(bottomSheetStatus == 4 ? View.VISIBLE : View.GONE);
        mTimeout.setVisibility(bottomSheetStatus == 3 && mCurrentRide.getDriverId() == null ?
                View.VISIBLE : View.GONE);
    }

    private void loadUserProfile() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference().child("Users").child("Customers").child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                View header = navigationView.getHeaderView(0);
                TextView usernameDrawer = header.findViewById(R.id.usernameDrawer);
                ImageView imageViewDrawer = header.findViewById(R.id.imageViewDrawer);

                String name = snapshot.child("name").getValue(String.class);
                String image = snapshot.child("profileImageUrl").getValue(String.class);

                usernameDrawer.setText(name != null ? name : "User");

                if (image != null && !image.equals("default") && !image.isEmpty()) {
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
                    mCurrentRide.setDriverId(driverId);
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
        if (requestBol) {
            Toast.makeText(this, "Cannot logout during active ride", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(this, LauncherActivity.class));
        finish();
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
                autocompleteFragmentTo.setText(address);

                if (destinationMarker != null) destinationMarker.remove();
                destinationMarker = mMap.addMarker(new MarkerOptions()
                        .position(destinationLatLng)
                        .title("Destination")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                if (pickupLatLng != null) {
                    calculateRoute();
                    showBottomSheet(2);
                }
            } else if (requestCode == AUTOCOMPLETE_REQUEST_CODE_FROM) {
                pickupLatLng = latLng;
                pickupAddress = address;
                autocompleteFragmentFrom.setText(address);

                if (pickupMarker != null) pickupMarker.remove();
                pickupMarker = mMap.addMarker(new MarkerOptions()
                        .position(pickupLatLng)
                        .title("Pickup")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                if (destinationLatLng != null) {
                    calculateRoute();
                    showBottomSheet(2);
                }
            }
        } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
            Status status = Autocomplete.getStatusFromIntent(data);
            Log.e("Places Error", status.getStatusMessage());
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.history) {
            startActivity(new Intent(this, HistoryActivity.class)
                    .putExtra("userType", "Customers"));
        } else if (id == R.id.settings) {
            startActivity(new Intent(this, CustomerSettingsActivity.class));
        } else if (id == R.id.payment) {
            startActivity(new Intent(this, PaymentActivity.class));
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
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
    }
}