package com.example.quickride.driver;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quickride.R;
import com.example.quickride.adapters.FixedRouteBookingAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ViewFixedRouteBookingsActivity extends AppCompatActivity {

    private RecyclerView rvBookings;
    private TextView tvNoBookings;
    private FixedRouteBookingAdapter adapter;
    private List<Map<String, Object>> bookingList;
    private com.example.quickride.models.FixedRoute activeRoute;
    private android.widget.Button btnCompleteJourney;
    private DatabaseReference bookingsRef;
    private String driverId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_fixed_route_bookings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvBookings = findViewById(R.id.rvBookings);
        tvNoBookings = findViewById(R.id.tvNoBookings);
        btnCompleteJourney = findViewById(R.id.btnCompleteJourney);

        btnCompleteJourney.setOnClickListener(v -> confirmCompleteJourney());


        bookingList = new ArrayList<>();
        adapter = new FixedRouteBookingAdapter(bookingList, booking -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Booking")
                    .setMessage("Remove this booking from your list?")
                    .setPositiveButton("Remove", (dialog, which) -> deleteBooking(booking))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        rvBookings.setAdapter(adapter);


        driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        bookingsRef = FirebaseDatabase.getInstance().getReference().child("FixedRouteBookings").child(driverId);

        loadActiveRoute();
        loadBookings();
    }

    private void loadActiveRoute() {
        DatabaseReference routesRef = FirebaseDatabase.getInstance().getReference().child("FixedRoutes");
        routesRef.orderByChild("driverId").equalTo(driverId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    com.example.quickride.models.FixedRoute route = data.getValue(com.example.quickride.models.FixedRoute.class);
                    if (route != null && route.isActive()) {
                        activeRoute = route;
                        updateCompleteButtonVisibility();
                        break;
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateCompleteButtonVisibility() {
        if (activeRoute != null && !bookingList.isEmpty()) {
            btnCompleteJourney.setVisibility(View.VISIBLE);
        } else {
            btnCompleteJourney.setVisibility(View.GONE);
        }
    }

    private void confirmCompleteJourney() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Complete Journey")
                .setMessage("Are you sure you want to finish this journey? This will save the ride to history for all passengers.")
                .setPositiveButton("Finish & Save", (dialog, which) -> finishJourney())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void finishJourney() {
        if (activeRoute == null || bookingList.isEmpty()) return;

        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("History");
        
        for (Map<String, Object> booking : bookingList) {
            String riderId = (String) booking.get("riderId");
            String riderName = (String) booking.get("riderName");
            String riderPhone = (String) booking.get("riderPhone");

            String rideId = historyRef.push().getKey();
            if (rideId == null) rideId = java.util.UUID.randomUUID().toString();

            Map<String, Object> historyData = new java.util.HashMap<>();
            historyData.put("rideId", rideId);
            historyData.put("driverId", driverId);
            historyData.put("customerId", riderId);
            historyData.put("driverName", activeRoute.getDriverName());
            historyData.put("customerName", riderName);
            historyData.put("customerPhone", riderPhone);
            historyData.put("pickupAddress", activeRoute.getStartPoint());
            historyData.put("destinationAddress", activeRoute.getDestination());
            historyData.put("fare", activeRoute.getFixedFare());
            historyData.put("distance", activeRoute.getDistance());
            historyData.put("timestamp", System.currentTimeMillis());
            historyData.put("status", "completed");
            historyData.put("paymentMethod", "Cash"); // Default for fixed routes
            
            historyRef.child(rideId).setValue(historyData);
        }

        // Mark route as inactive and delete bookings
        FirebaseDatabase.getInstance().getReference().child("FixedRoutes").child(activeRoute.getRouteId()).child("active").setValue(false);
        bookingsRef.removeValue();
        
        android.widget.Toast.makeText(this, "Journey completed! History saved.", android.widget.Toast.LENGTH_LONG).show();
        finish();
    }


    private void loadBookings() {
        bookingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookingList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Map<String, Object> booking = (Map<String, Object>) data.getValue();
                    if (booking != null) {
                        booking.put("bookingId", data.getKey()); // Capture the unique Firebase push key
                        bookingList.add(booking);
                    }
                }
                adapter.notifyDataSetChanged();
                updateCompleteButtonVisibility();
                
                if (bookingList.isEmpty()) {

                    tvNoBookings.setVisibility(View.VISIBLE);
                    rvBookings.setVisibility(View.GONE);
                } else {
                    tvNoBookings.setVisibility(View.GONE);
                    rvBookings.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void deleteBooking(Map<String, Object> booking) {
        if (booking == null || !booking.containsKey("bookingId")) return;
        
        String bookingId = (String) booking.get("bookingId");
        bookingsRef.child(bookingId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                android.widget.Toast.makeText(this, "Booking removed", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }
}

