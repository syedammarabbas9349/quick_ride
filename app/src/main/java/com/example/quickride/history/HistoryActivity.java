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
import android.widget.ProgressBar;
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
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private TextView tvUserType;

    private String userType;
    private String idField;
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

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(com.example.quickride.R.menu.history_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == com.example.quickride.R.id.clear_history) {
            confirmClearHistory();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmClearHistory() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear History")
                .setMessage("Are you sure you want to delete all your ride history? This action cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> clearAllHistory())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAllHistory() {
        showLoading(true);
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("History");
        Query query = historyRef.orderByChild(idField).equalTo(currentUserId);
        
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    child.getRef().removeValue();
                }
                
                // Also clear legacy ride_info
                DatabaseReference legacyRef = FirebaseDatabase.getInstance().getReference().child("ride_info");
                Query legacyQuery = legacyRef.orderByChild(idField).equalTo(currentUserId);
                legacyQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            child.getRef().removeValue();
                        }
                        historyList.clear();
                        historyAdapter.notifyDataSetChanged();
                        updateEmptyState();
                        showLoading(false);
                        android.widget.Toast.makeText(HistoryActivity.this, "History cleared successfully", android.widget.Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showLoading(false);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
            }
        });
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
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userType = getIntent().getStringExtra("userType");

        if (userType == null) {
            userType = "Customers";
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

        // ── Read from the dedicated 'History' root node ─────────────────────
        // Falls back to 'ride_info' if 'History' is empty (backwards compat)
        DatabaseReference historyRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("History");

        Query query = historyRef.orderByChild(idField).equalTo(currentUserId);

        query.addChildEventListener(new ChildEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                showLoading(false);
                if (!snapshot.exists()) return;

                RideHistory ride = parseRideFromSnapshot(snapshot);

                // All entries in History are completed – no status filter needed
                if (ride != null) {
                    historyList.add(0, ride);
                    historyAdapter.notifyDataSetChanged();
                    updateEmptyState();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                // Fallback: try ride_info if History node is unavailable
                loadHistoryFallback();
            }
        });

        // Also load legacy rides from ride_info that predate the History node.
        // The fallback method deduplicates by rideId automatically.
        loadHistoryFallback();
    }

    /**
     * Fallback: reads from ride_info for rides completed before the History node was introduced.
     * Only adds rides not already present in historyList.
     */
    @SuppressLint("NotifyDataSetChanged")
    private void loadHistoryFallback() {
        DatabaseReference fallbackRef = FirebaseDatabase.getInstance()
                .getReference().child("ride_info");

        Query fallbackQuery = fallbackRef.orderByChild(idField).equalTo(currentUserId);
        fallbackQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    RideHistory ride = parseRideFromSnapshot(child);
                    if (ride == null) continue;
                    if ("pending".equals(ride.getStatus())) continue;

                    // Avoid duplicates already loaded from History node
                    boolean alreadyPresent = false;
                    for (RideHistory r : historyList) {
                        if (r.getTimestamp() == ride.getTimestamp() && 
                            r.getCustomerId() != null && 
                            r.getCustomerId().equals(ride.getCustomerId())) {
                            alreadyPresent = true;
                            break;
                        }
                    }
                    if (!alreadyPresent) {
                        historyList.add(0, ride);
                    }
                }
                historyAdapter.notifyDataSetChanged();
                updateEmptyState();
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
            }
        });
    }

    private RideHistory parseRideFromSnapshot(DataSnapshot snapshot) {
        RideHistory ride = new RideHistory();
        ride.setRideId(snapshot.getKey());

        // Timestamp
        if (snapshot.child("timestamp").getValue() != null) {
            ride.setTimestamp(Long.parseLong(snapshot.child("timestamp").getValue().toString()));
        }

        // Customer
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

        // Driver
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

        // Addresses
        if (snapshot.child("pickupAddress").getValue() != null) {
            ride.setPickupAddress(snapshot.child("pickupAddress").getValue(String.class));
        }

        if (snapshot.child("destinationAddress").getValue() != null) {
            ride.setDestinationAddress(snapshot.child("destinationAddress").getValue(String.class));
        }

        // Coordinates
        if (snapshot.child("pickupLat").getValue() != null) {
            ride.setPickupLat(Double.parseDouble(snapshot.child("pickupLat").getValue().toString()));
        }

        if (snapshot.child("pickupLng").getValue() != null) {
            ride.setPickupLng(Double.parseDouble(snapshot.child("pickupLng").getValue().toString()));
        }

        if (snapshot.child("destLat").getValue() != null) {
            ride.setDestLat(Double.parseDouble(snapshot.child("destLat").getValue().toString()));
        }

        if (snapshot.child("destLng").getValue() != null) {
            ride.setDestLng(Double.parseDouble(snapshot.child("destLng").getValue().toString()));
        }

        // Ride details
        if (snapshot.child("distance").getValue() != null) {
            ride.setDistance(Double.parseDouble(snapshot.child("distance").getValue().toString()));
        }

        if (snapshot.child("fare").getValue() != null) {
            ride.setFare(Double.parseDouble(snapshot.child("fare").getValue().toString()));
        }

        if (snapshot.child("paymentMethod").getValue() != null) {
            ride.setPaymentMethod(snapshot.child("paymentMethod").getValue(String.class));
        }

        if (snapshot.child("status").getValue() != null) {
            ride.setStatus(snapshot.child("status").getValue(String.class));
        }

        if (snapshot.child("rating").getValue() != null) {
            ride.setRating(Double.parseDouble(snapshot.child("rating").getValue().toString()));
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