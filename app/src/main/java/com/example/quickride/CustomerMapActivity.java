package com.example.quickride;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private Button mLogout, mRequest, mSettings;
    private LatLng pickupLocation;
    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundID;

    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;
    private Marker mDriverMarker, pickupMarker;
    private Boolean requestBol = false;
    private GeoQuery geoQuery;

    private View mDriverInfo;
    private ImageView mDriverProfileImage;
    private TextView mDriverName, mDriverPhone, mDriverCar;

    private String destination;
    private boolean isSearching = false;

    // Permission Constant
    final int LOCATION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyCecHc-qcR5XFROvltXJXtvt58HI4DR11o");
        }

        mDriverInfo = findViewById(R.id.driverInfo);
        mDriverProfileImage = findViewById(R.id.driverProfileImage);
        mDriverName = findViewById(R.id.driverName);
        mDriverPhone = findViewById(R.id.driverPhone);
        mDriverCar = findViewById(R.id.driverCar);
        mLogout = findViewById(R.id.logout);
        mRequest = findViewById(R.id.request);
        mSettings = findViewById(R.id.settings);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) { mapFragment.getMapAsync(this); }

        setupAutocomplete();

        mLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        mSettings.setOnClickListener(v -> startActivity(new Intent(CustomerMapActivity.this, CustomerSettingsActivity.class)));

        mRequest.setOnClickListener(v -> {
            if (requestBol) { endRide(); }
            else {
                requestBol = true;
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
                GeoFire geoFire = new GeoFire(ref);

                if (mLastLocation != null) {
                    geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), (key, error) -> {});
                    pickupLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Here"));

                    mRequest.setText("Getting your Driver...");
                    getClosestDriver();
                } else {
                    Toast.makeText(this, "Location not found yet. Please wait.", Toast.LENGTH_SHORT).show();
                    requestBol = false;
                }
            }
        });
    }

    private void setupAutocomplete() {
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        if (autocompleteFragment != null && autocompleteFragment.getView() != null) {
            autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
            autocompleteFragment.getView().setOnClickListener(v -> isSearching = true);

            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    destination = place.getName();
                    isSearching = false;
                    if (place.getLatLng() != null) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15));
                    }
                }
                @Override public void onError(@NonNull Status status) {
                    isSearching = false;
                    Log.e("QuickRide", "Places Error: " + status);
                }
            });
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        mLastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (!requestBol && !isSearching) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        }
    }

    private void getClosestDriver() {
        DatabaseReference driverAvailable = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
        GeoFire geoFire = new GeoFire(driverAvailable);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && requestBol) {
                    driverFound = true;
                    driverFoundID = key;

                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("customerRideID", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    map.put("destination", destination != null ? destination : "No destination set");
                    driverRef.updateChildren(map);

                    getDriverLocation();
                    getDriverInfo();
                    mRequest.setText("Looking for Driver Location...");
                }
            }
            @Override public void onGeoQueryReady() {
                if (!driverFound && radius < 20) { radius++; getClosestDriver(); }
            }
            @Override public void onKeyExited(String key) {}
            @Override public void onKeyMoved(String key, GeoLocation location) {}
            @Override public void onGeoQueryError(DatabaseError error) {}
        });
    }

    private void getDriverLocation() {
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driverFoundID).child("l");
        driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBol) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = Double.parseDouble(map.get(0).toString());
                    double locationLng = Double.parseDouble(map.get(1).toString());

                    LatLng driverLatLng = new LatLng(locationLat, locationLng);
                    if (mDriverMarker != null) mDriverMarker.remove();

                    Location loc1 = new Location(""); loc1.setLatitude(pickupLocation.latitude); loc1.setLongitude(pickupLocation.longitude);
                    Location loc2 = new Location(""); loc2.setLatitude(driverLatLng.latitude); loc2.setLongitude(driverLatLng.longitude);

                    float distance = loc1.distanceTo(loc2);
                    mRequest.setText(distance < 100 ? "Driver's Here" : "Driver Found: " + String.format("%.0f", distance) + "m");

                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your Driver"));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void getDriverInfo() {
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mDriverDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
        mDriverDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if(map.get("name") != null) mDriverName.setText(map.get("name").toString());
                    if(map.get("phone") != null) mDriverPhone.setText(map.get("phone").toString());
                    if(map.get("car") != null) mDriverCar.setText(map.get("car").toString());
                    if(map.get("profileImageUrl") != null) Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mDriverProfileImage);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void endRide() {
        requestBol = false;
        if (geoQuery != null) geoQuery.removeAllListeners();
        if (driverLocationRef != null) driverLocationRef.removeEventListener(driverLocationRefListener);
        if (driverFoundID != null) {
            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
            Map<String, Object> map = new HashMap<>();
            map.put("customerRideID", "");
            map.put("destination", "");
            driverRef.updateChildren(map);
        }
        driverFound = false;
        radius = 1;
        new GeoFire(FirebaseDatabase.getInstance().getReference("customerRequest")).removeLocation(FirebaseAuth.getInstance().getCurrentUser().getUid());

        if (pickupMarker != null) pickupMarker.remove();
        if (mDriverMarker != null) mDriverMarker.remove();

        mRequest.setText("Call QuickRide");
        mDriverInfo.setVisibility(View.GONE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        mGoogleApiClient.connect();
    }

    @Override public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mGoogleApiClient == null) {
                    buildGoogleApiClient();
                }
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override public void onConnectionSuspended(int i) { if (mGoogleApiClient != null) mGoogleApiClient.connect(); }
    @Override public void onConnectionFailed(@NonNull ConnectionResult result) {}
}