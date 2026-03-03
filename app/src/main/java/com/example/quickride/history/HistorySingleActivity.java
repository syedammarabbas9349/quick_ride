package com.example.quickride.history;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.quickride.R;
import com.example.quickride.models.RideHistory;
import com.example.quickride.models.User;
import com.example.quickride.utils.RouteHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Activity that displays a single ride history in detail
 */
public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback {

    private String rideId;
    private String currentUserId;
    private String userType; // "customer" or "driver"
    private RideHistory rideData;

    // UI Components
    private Toolbar toolbar;
    private GoogleMap mMap;
    private List<Polyline> polylines = new ArrayList<>();

    // Map markers
    private LatLng pickupLatLng;
    private LatLng destLatLng;

    // Text views
    private TextView tvDateTime, tvPickup, tvDestination, tvDistance, tvDuration, tvFare;
    private TextView tvPaymentMethod, tvUserName, tvUserPhone, tvUserRating;
    private ImageView ivUserImage;
    private RatingBar ratingBar;
    private LinearLayout ratingContainer;
    private MaterialButton btnCall, btnMessage, btnShare;
    private MaterialCardView userInfoCard;

    // Route helper
    private RouteHelper routeHelper;

    public static void start(AppCompatActivity activity, String rideId, String userType) {
        Intent intent = new Intent(activity, HistorySingleActivity.class);
        intent.putExtra("rideId", rideId);
        intent.putExtra("userType", userType);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);

        // Get intent data
        rideId = getIntent().getStringExtra("rideId");
        userType = getIntent().getStringExtra("userType");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initializeViews();
        setupToolbar();
        setupMap();
        setupRouteHelper();
        loadRideData();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tvDateTime = findViewById(R.id.dateTime);
        tvPickup = findViewById(R.id.pickupLocation);
        tvDestination = findViewById(R.id.destinationLocation);
        tvDistance = findViewById(R.id.distance);
        tvDuration = findViewById(R.id.duration);
        tvFare = findViewById(R.id.fare);
        tvPaymentMethod = findViewById(R.id.paymentMethod);
        tvUserName = findViewById(R.id.userName);
        tvUserPhone = findViewById(R.id.userPhone);
        tvUserRating = findViewById(R.id.userRating);
        ivUserImage = findViewById(R.id.userImage);
        ratingBar = findViewById(R.id.ratingBar);
        ratingContainer = findViewById(R.id.ratingContainer);
        btnCall = findViewById(R.id.btnCall);
        btnMessage = findViewById(R.id.btnMessage);
        btnShare = findViewById(R.id.btnShare);
        userInfoCard = findViewById(R.id.userInfoCard);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.ride_details);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupRouteHelper() {
        routeHelper = new RouteHelper(this, getString(R.string.google_maps_key));
    }

    private void loadRideData() {
        DatabaseReference rideRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("ride_info")
                .child(rideId);

        rideRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(HistorySingleActivity.this,
                            R.string.ride_not_found, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                rideData = parseRideData(snapshot);
                displayRideInfo();
                loadOtherUserInfo();
                setupRating();

                // Setup map markers
                if (mMap != null) {
                    addMapMarkers();
                    drawRoute();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HistorySingleActivity.this,
                        R.string.error_loading + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private RideHistory parseRideData(DataSnapshot snapshot) {
        RideHistory ride = new RideHistory();
        ride.setRideId(snapshot.getKey());

        // Parse timestamps
        if (snapshot.child("timestamp").getValue() != null) {
            ride.setTimestamp(snapshot.child("timestamp").getValue(Long.class));
        }

        // Parse locations
        if (snapshot.child("pickupAddress").getValue() != null) {
            ride.setPickupAddress(snapshot.child("pickupAddress").getValue(String.class));
        }
        if (snapshot.child("destinationAddress").getValue() != null) {
            ride.setDestinationAddress(snapshot.child("destinationAddress").getValue(String.class));
        }
        if (snapshot.child("pickupLat").getValue() != null) {
            ride.setPickupLat(snapshot.child("pickupLat").getValue(Double.class));
        }
        if (snapshot.child("pickupLng").getValue() != null) {
            ride.setPickupLng(snapshot.child("pickupLng").getValue(Double.class));
        }
        if (snapshot.child("destLat").getValue() != null) {
            ride.setDestLat(snapshot.child("destLat").getValue(Double.class));
        }
        if (snapshot.child("destLng").getValue() != null) {
            ride.setDestLng(snapshot.child("destLng").getValue(Double.class));
        }

        // Parse ride details
        if (snapshot.child("distance").getValue() != null) {
            ride.setDistance(snapshot.child("distance").getValue(Double.class));
        }
        if (snapshot.child("duration").getValue() != null) {
            ride.setDuration(snapshot.child("duration").getValue(String.class));
        }
        if (snapshot.child("fare").getValue() != null) {
            ride.setFare(snapshot.child("fare").getValue(Double.class));
        }
        if (snapshot.child("paymentMethod").getValue() != null) {
            ride.setPaymentMethod(snapshot.child("paymentMethod").getValue(String.class));
        }
        if (snapshot.child("rating").getValue() != null) {
            ride.setRating(snapshot.child("rating").getValue(Double.class));
        }
        if (snapshot.child("car").getValue() != null) {
            ride.setCarInfo(snapshot.child("car").getValue(String.class));
        }

        // Parse customer info
        if (snapshot.child("customerId").getValue() != null) {
            ride.setCustomerId(snapshot.child("customerId").getValue(String.class));
        }
        if (snapshot.child("customerName").getValue() != null) {
            ride.setCustomerName(snapshot.child("customerName").getValue(String.class));
        }
        if (snapshot.child("customerPhone").getValue() != null) {
            ride.setCustomerPhone(snapshot.child("customerPhone").getValue(String.class));
        }
        if (snapshot.child("customerImageUrl").getValue() != null) {
            ride.setCustomerImageUrl(snapshot.child("customerImageUrl").getValue(String.class));
        }

        // Parse driver info
        if (snapshot.child("driverId").getValue() != null) {
            ride.setDriverId(snapshot.child("driverId").getValue(String.class));
        }
        if (snapshot.child("driverName").getValue() != null) {
            ride.setDriverName(snapshot.child("driverName").getValue(String.class));
        }
        if (snapshot.child("driverPhone").getValue() != null) {
            ride.setDriverPhone(snapshot.child("driverPhone").getValue(String.class));
        }
        if (snapshot.child("driverImageUrl").getValue() != null) {
            ride.setDriverImageUrl(snapshot.child("driverImageUrl").getValue(String.class));
        }

        return ride;
    }

    @SuppressLint("SetTextI18n")
    private void displayRideInfo() {
        if (rideData == null) return;

        // Format date and time
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        tvDateTime.setText(sdf.format(new Date(rideData.getTimestamp())));

        // Set locations
        tvPickup.setText(rideData.getPickupAddress());
        tvDestination.setText(rideData.getDestinationAddress());

        // Set distance
        tvDistance.setText(String.format(Locale.getDefault(),
                "%.1f km", rideData.getDistance()));

        // Set duration
        tvDuration.setText(rideData.getDuration() != null ?
                rideData.getDuration() : "30 min");

        // Set fare
        tvFare.setText(String.format(Locale.getDefault(),
                "Rs. %.0f", rideData.getFare()));

        // Set payment method with icon
        String paymentText = rideData.getPaymentMethod();
        if (paymentText != null) {
            switch (paymentText.toLowerCase()) {
                case "jazzcash":
                    tvPaymentMethod.setText("JazzCash");
                    tvPaymentMethod.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_jazzcash, 0, 0, 0);
                    break;
                case "easypaisa":
                    tvPaymentMethod.setText("EasyPaisa");
                    tvPaymentMethod.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_easypaisa, 0, 0, 0);
                    break;
                case "cash":
                    tvPaymentMethod.setText("Cash");
                    tvPaymentMethod.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_cash, 0, 0, 0);
                    break;
                default:
                    tvPaymentMethod.setText(paymentText);
                    break;
            }
        }

        // Set pickup and destination coordinates for map
        pickupLatLng = new LatLng(rideData.getPickupLat(), rideData.getPickupLng());
        destLatLng = new LatLng(rideData.getDestLat(), rideData.getDestLng());
    }

    private void loadOtherUserInfo() {
        String otherUserId;
        String otherUserType;

        // Determine other user (the one who is not current user)
        if (userType != null && userType.equals("driver")) {
            otherUserId = rideData.getCustomerId();
            otherUserType = "Customers";
        } else {
            otherUserId = rideData.getDriverId();
            otherUserType = "Drivers";
        }

        if (otherUserId == null) return;

        DatabaseReference otherUserRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child(otherUserType)
                .child(otherUserId);

        otherUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String name = snapshot.child("name").getValue(String.class);
                String phone = snapshot.child("phone").getValue(String.class);
                String imageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                Double rating = snapshot.child("rating").getValue(Double.class);

                tvUserName.setText(name != null ? name : "User");
                tvUserPhone.setText(phone != null ? phone : "Phone not available");

                if (rating != null) {
                    tvUserRating.setText(String.format(Locale.getDefault(),
                            "%.1f ★", rating));
                }

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Glide.with(HistorySingleActivity.this)
                            .load(imageUrl)
                            .apply(RequestOptions.circleCropTransform())
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .into(ivUserImage);
                }

                // Setup call button
                if (phone != null && !phone.isEmpty()) {
                    btnCall.setOnClickListener(v -> dialPhoneNumber(phone));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupRating() {
        // Show rating bar only for customers rating drivers
        if (userType != null && userType.equals("customer")) {
            ratingContainer.setVisibility(View.VISIBLE);
            ratingBar.setRating((float) rideData.getRating());

            ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
                if (fromUser) {
                    saveRating(rating);
                }
            });
        } else {
            ratingContainer.setVisibility(View.GONE);
        }
    }

    private void saveRating(float rating) {
        DatabaseReference rideRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("ride_info")
                .child(rideId);

        rideRef.child("rating").setValue(rating)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.rating_saved, Toast.LENGTH_SHORT).show();

                    // Update driver's average rating
                    if (rideData.getDriverId() != null) {
                        updateDriverRating(rating);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.error_saving_rating, Toast.LENGTH_SHORT).show());
    }

    private void updateDriverRating(float newRating) {
        DatabaseReference driverRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child("Drivers")
                .child(rideData.getDriverId());

        driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                double currentRating = snapshot.child("rating").exists() ?
                        snapshot.child("rating").getValue(Double.class) : 5.0;
                long totalRides = snapshot.child("totalRides").exists() ?
                        snapshot.child("totalRides").getValue(Long.class) : 0;

                // Calculate new average
                double newAverage = ((currentRating * totalRides) + newRating) / (totalRides + 1);

                driverRef.child("rating").setValue(newAverage);
                driverRef.child("totalRides").setValue(totalRides + 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void dialPhoneNumber(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }

    private void addMapMarkers() {
        if (mMap == null || pickupLatLng == null || destLatLng == null) return;

        // Clear existing markers
        mMap.clear();

        // Add pickup marker (green)
        mMap.addMarker(new MarkerOptions()
                .position(pickupLatLng)
                .title("Pickup")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // Add destination marker (red)
        mMap.addMarker(new MarkerOptions()
                .position(destLatLng)
                .title("Destination")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
    }

    private void drawRoute() {
        if (pickupLatLng == null || destLatLng == null) return;

        routeHelper.getRoute(pickupLatLng, destLatLng, new RouteHelper.RouteCallback() {
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
                        .width(8)
                        .color(ContextCompat.getColor(HistorySingleActivity.this, R.color.colorPrimary))
                        .geodesic(true);

                polylines.add(mMap.addPolyline(options));

                // Zoom to show entire route
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(pickupLatLng);
                builder.include(destLatLng);
                LatLngBounds bounds = builder.build();

                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
            }

            @Override
            public void onRouteFailure(String error) {
                // Just zoom to show both markers
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(pickupLatLng);
                builder.include(destLatLng);
                LatLngBounds bounds = builder.build();

                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setAllGesturesEnabled(false);
        if (pickupLatLng != null && destLatLng != null) {
            addMapMarkers();
            drawRoute();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}