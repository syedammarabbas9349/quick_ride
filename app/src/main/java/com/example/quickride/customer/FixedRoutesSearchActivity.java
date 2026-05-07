package com.example.quickride.customer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quickride.R;
import com.example.quickride.adapters.FixedRouteAdapter;
import com.example.quickride.models.FixedRoute;
import com.example.quickride.utils.PaymentUtils;
import com.example.quickride.utils.NotificationHelper;
import android.widget.Button;
import android.widget.EditText;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class FixedRoutesSearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private Button btnSearch;
    private RecyclerView rvFixedRoutes;
    private TextView tvNoRoutes;
    private FixedRouteAdapter adapter;
    private List<FixedRoute> routeList;
    private DatabaseReference routesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fixed_routes_search);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Carpooling / Fixed Routes");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        rvFixedRoutes = findViewById(R.id.rvFixedRoutes);
        tvNoRoutes = findViewById(R.id.tvNoRoutes);

        routeList = new ArrayList<>();
        adapter = new FixedRouteAdapter(routeList, this, this::joinSeat);
        rvFixedRoutes.setLayoutManager(new LinearLayoutManager(this));
        rvFixedRoutes.setAdapter(adapter);

        routesRef = FirebaseDatabase.getInstance().getReference().child("FixedRoutes");

        loadAllRoutes();

        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim().toLowerCase();
            filterRoutes(query);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRoutes(s.toString().trim().toLowerCase());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadAllRoutes() {
        routesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                routeList.clear();
                Log.d("FixedRouteSearch", "Snapshot exists: " + snapshot.exists() + ", Children count: " + snapshot.getChildrenCount());
                for (DataSnapshot data : snapshot.getChildren()) {
                    FixedRoute route = data.getValue(FixedRoute.class);
                    if (route != null) {
                        Log.d("FixedRouteSearch", "Route found: " + route.getDestination() + ", Active: " + route.isActive());
                        if (route.isActive() && route.getAvailableSeats() > 0) {
                            routeList.add(route);
                        }

                    }
                }
                adapter.notifyDataSetChanged();
                if (routeList.isEmpty()) {
                    tvNoRoutes.setVisibility(View.VISIBLE);
                    rvFixedRoutes.setVisibility(View.GONE);
                } else {
                    tvNoRoutes.setVisibility(View.GONE);
                    rvFixedRoutes.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FixedRoutesSearchActivity.this, "Error loading routes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterRoutes(String query) {
        if (query.isEmpty()) {
            loadAllRoutes();
            return;
        }

        routesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                routeList.clear();
                String queryStripped = query.replace(" ", "");
                for (DataSnapshot data : snapshot.getChildren()) {
                    FixedRoute route = data.getValue(FixedRoute.class);
                    if (route != null && route.isActive() && route.getAvailableSeats() > 0) {

                        String dest = (route.getDestination() != null) ? route.getDestination().toLowerCase() : "";
                        String start = (route.getStartPoint() != null) ? route.getStartPoint().toLowerCase() : "";
                        
                        String destStripped = dest.replace(" ", "");
                        String startStripped = start.replace(" ", "");

                        boolean matchesDest = dest.contains(query) || destStripped.contains(queryStripped);
                        boolean matchesStart = start.contains(query) || startStripped.contains(queryStripped);
                        
                        if (matchesDest || matchesStart) {
                            routeList.add(route);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                if (routeList.isEmpty()) {
                    tvNoRoutes.setVisibility(View.VISIBLE);
                    rvFixedRoutes.setVisibility(View.GONE);
                } else {
                    tvNoRoutes.setVisibility(View.GONE);
                    rvFixedRoutes.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void joinSeat(FixedRoute route) {
        // Show Payment Selection Dialog first
        PaymentUtils.showPaymentSelectionDialog(this, (method, accountNumber) -> {
            executeJoinSeat(route, method, accountNumber);
        });
    }

    private void executeJoinSeat(FixedRoute route, String paymentMethod, String accountNumber) {
        String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference specificRouteRef = routesRef.child(route.getRouteId());

        specificRouteRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                FixedRoute r = currentData.getValue(FixedRoute.class);
                if (r == null || !r.isActive()) {
                    return Transaction.success(currentData);
                }
                
                if (r.getAvailableSeats() > 0) {
                    r.setAvailableSeats(r.getAvailableSeats() - 1);
                    if (r.getAvailableSeats() == 0) {
                        r.setActive(false);
                    }
                    currentData.setValue(r);
                }
 else {
                    return Transaction.abort(); // No seats left
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Toast.makeText(FixedRoutesSearchActivity.this, "Failed to join: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                } else if (!committed) {
                    Toast.makeText(FixedRoutesSearchActivity.this, "No seats available!", Toast.LENGTH_SHORT).show();
                } else {
                    saveBookingDetails(route, paymentMethod, accountNumber);
                    updateDriverEarnings(route.getDriverId(), route.getFixedFare(), route.getDistance());
                    
                    // Check if route is now full
                    if (currentData != null && currentData.exists()) {
                        FixedRoute updatedRoute = currentData.getValue(FixedRoute.class);
                        if (updatedRoute != null && updatedRoute.getAvailableSeats() == 0) {
                            NotificationHelper.getInstance(FixedRoutesSearchActivity.this)
                                    .sendRouteFullNotification(updatedRoute.getDriverId(), 
                                            updatedRoute.getStartPoint() + " to " + updatedRoute.getDestination());
                        }
                    }
                }
            }
        });
    }

    private void saveBookingDetails(FixedRoute route, String paymentMethod, String accountNumber) {
        String riderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference riderRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(riderId);
        
        riderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String riderName = "Rider";
                String riderPhone = "";
                if (snapshot.exists()) {
                    if (snapshot.child("name").exists()) riderName = snapshot.child("name").getValue().toString();
                    if (snapshot.child("phone").exists()) riderPhone = snapshot.child("phone").getValue().toString();
                }

                DatabaseReference bookingsRef = FirebaseDatabase.getInstance().getReference()
                        .child("FixedRouteBookings")
                        .child(route.getDriverId())
                        .push();

                java.util.Map<String, Object> bookingData = new java.util.HashMap<>();
                bookingData.put("riderId", riderId);
                bookingData.put("riderName", riderName);
                bookingData.put("riderPhone", riderPhone);
                bookingData.put("timestamp", System.currentTimeMillis());
                bookingData.put("routeInfo", route.getStartPoint() + " to " + route.getDestination());
                bookingData.put("paymentMethod", paymentMethod);
                if (accountNumber != null) {
                    bookingData.put("transactionId", accountNumber);
                }

                bookingsRef.setValue(bookingData).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(FixedRoutesSearchActivity.this, "Seat booked successfully!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {}
        });

        // Also update stats for carpooling rides
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
            @Override public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {}
        });
    }
}
