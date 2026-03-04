package com.example.quickride.history;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickride.R;
import com.example.quickride.adapters.HistoryAdapter;
import com.example.quickride.models.RideHistory;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays ride history for both customers and drivers
 */
public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter historyAdapter;
    private List<RideHistory> historyList = new ArrayList<>();
    private LinearLayout emptyLayout;
    private CircularProgressIndicator progressBar;
    private Toolbar toolbar;
    private TextView tvUserType;

    private String userType; // "Customers" or "Drivers"
    private String idField; // "customerId" or "driverId"
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        getUserData();
        loadHistory();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        emptyLayout = findViewById(R.id.emptyLayout);
        progressBar = findViewById(R.id.progressBar);
        toolbar = findViewById(R.id.toolbar);
        tvUserType = findViewById(R.id.userType);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.your_trips);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        historyAdapter = new HistoryAdapter(historyList, this, new HistoryAdapter.OnHistoryItemClickListener() {
            @Override
            public void onItemClick(RideHistory ride, int position) {
                // Open history detail
                Intent intent = new Intent(HistoryActivity.this, HistorySingleActivity.class);
                intent.putExtra("rideId", ride.getRideId());
                intent.putExtra("userType", userType);
                startActivity(intent);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(historyAdapter);
    }

    private void getUserData() {
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userType = getIntent().getStringExtra("userType");

        if (userType == null) {
            userType = "Customers"; // Default
        }

        if (userType.equals("Drivers")) {
            idField = "driverId";
            tvUserType.setText(R.string.driver_trips);
        } else {
            idField = "customerId";
            tvUserType.setText(R.string.your_trips);
        }
    }

    private void loadHistory() {
        showLoading(true);

        DatabaseReference historyRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("ride_info");

        Query query = historyRef.orderByChild(idField).equalTo(currentUserId);

        query.addChildEventListener(new ChildEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (!snapshot.exists()) return;

                RideHistory ride = parseRideFromSnapshot(snapshot);

                // Only show completed/cancelled rides
                if (ride != null && !"pending".equals(ride.getStatus())) {
                    historyList.add(0, ride);
                    historyAdapter.notifyDataSetChanged();
                    updateEmptyState();
                }

                showLoading(false);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Handle ride updates if needed
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                // Handle ride removal if needed
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Not needed
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                // Handle error
            }
        });
    }

    private RideHistory parseRideFromSnapshot(DataSnapshot snapshot) {
        RideHistory ride = new RideHistory();
        ride.setRideId(snapshot.getKey());

        // Parse basic info
        if (snapshot.child("timestamp").getValue() != null) {
            ride.setTimestamp(snapshot.child("timestamp").getValue(Long.class));
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
        if (snapshot.child("car").getValue() != null) {
            ride.setCarInfo(snapshot.child("car").getValue(String.class));
        }

        // Parse ride details
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
        if (snapshot.child("distance").getValue() != null) {
            ride.setDistance(snapshot.child("distance").getValue(Double.class));
        }
        if (snapshot.child("fare").getValue() != null) {
            ride.setFare(snapshot.child("fare").getValue(Double.class));
        }
        if (snapshot.child("paymentMethod").getValue() != null) {
            ride.setPaymentMethod(snapshot.child("paymentMethod").getValue(String.class));
        }
        if (snapshot.child("status").getValue() != null) {
            ride.setStatus(snapshot.child("status").getValue(String.class));
        }
        if (snapshot.child("rating").getValue() != null) {
            ride.setRating(snapshot.child("rating").getValue(Double.class));
        }

        return ride;
    }

    private void updateEmptyState() {
        if (historyList.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}