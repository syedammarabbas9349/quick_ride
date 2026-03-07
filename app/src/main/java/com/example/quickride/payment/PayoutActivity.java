package com.example.quickride.payment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickride.R;
import com.example.quickride.adapters.PayoutAdapter;
import com.example.quickride.models.Payout;
import com.example.quickride.models.User;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class PayoutActivity extends AppCompatActivity implements PayoutAdapter.OnItemClickListener {

    // UI
    private Toolbar toolbar;
    private TextView tvAvailableBalance, tvTotalEarnings, tvTotalRides, tvTotalDistance;
    private Button btnRequestPayout;
    private RecyclerView recyclerView;
    private CircularProgressIndicator progressBar;
    private View emptyState;
    private View mainLayout;

    // Data
    private PayoutAdapter payoutAdapter;
    private List<Payout> payoutList = new ArrayList<>();

    private DatabaseReference driverRef;
    private DatabaseReference payoutsRef;

    private String currentUserId;

    private double availableBalance = 0;
    private double totalEarnings = 0;
    private int totalRides = 0;
    private double totalDistance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payout);

        mainLayout = findViewById(R.id.layout);

        initializeViews();
        setupToolbar();
        setupFirebase();
        setupRecyclerView();

        loadDriverData();
        loadPayoutHistory();

        btnRequestPayout.setOnClickListener(v -> requestFullPayout());
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tvAvailableBalance = findViewById(R.id.tvAvailableBalance);
        tvTotalEarnings = findViewById(R.id.tvTotalEarnings);
        tvTotalRides = findViewById(R.id.tvTotalRides);
        tvTotalDistance = findViewById(R.id.tvTotalDistance);
        btnRequestPayout = findViewById(R.id.btnRequestPayout);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyState = findViewById(R.id.emptyState);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.earnings);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupFirebase() {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        driverRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child("Drivers")
                .child(currentUserId);

        payoutsRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Payouts")
                .child(currentUserId);
    }

    private void setupRecyclerView() {

        payoutAdapter = new PayoutAdapter(payoutList, this, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(payoutAdapter);
    }

    private void loadDriverData() {

        showLoading(true);

        driverRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    showLoading(false);
                    return;
                }

                loadEarningsData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                showError(error.getMessage());
            }
        });
    }

    private void loadEarningsData() {

        DatabaseReference earningsRef = driverRef.child("earnings");

        earningsRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                availableBalance = snapshot.child("available").exists()
                        ? snapshot.child("available").getValue(Double.class) : 0;

                totalEarnings = snapshot.child("total").exists()
                        ? snapshot.child("total").getValue(Double.class) : 0;

                updateEarningsDisplay();
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                showError(error.getMessage());
            }
        });

        DatabaseReference statsRef = driverRef.child("stats");

        statsRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                totalRides = snapshot.child("totalRides").exists()
                        ? snapshot.child("totalRides").getValue(Integer.class) : 0;

                totalDistance = snapshot.child("totalDistance").exists()
                        ? snapshot.child("totalDistance").getValue(Double.class) : 0;

                updateStatsDisplay();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadPayoutHistory() {

        Query query = payoutsRef.orderByChild("requestedAt").limitToLast(50);

        query.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                payoutList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {

                    Payout payout = dataSnapshot.getValue(Payout.class);

                    if (payout != null) {

                        payout.setPayoutId(dataSnapshot.getKey());
                        payoutList.add(0, payout);
                    }
                }

                payoutAdapter.notifyDataSetChanged();
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError(error.getMessage());
            }
        });
    }

    private void requestFullPayout() {

        if (availableBalance <= 0) {
            showError("No balance available for payout");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Request Payout")
                .setMessage("Withdraw Rs. " + availableBalance + "?")
                .setPositiveButton("Confirm", (dialog, which) -> {

                    Map<String, Object> payout = new HashMap<>();

                    payout.put("amount", availableBalance);
                    payout.put("status", "pending");
                    payout.put("requestedAt", System.currentTimeMillis());
                    payout.put("period", "Manual Request");
                    payout.put("rideCount", totalRides);

                    String payoutId = payoutsRef.push().getKey();

                    if (payoutId != null) {
                        payoutsRef.child(payoutId).setValue(payout);
                    }

                    Snackbar.make(mainLayout,
                            "Payout request created",
                            Snackbar.LENGTH_LONG).show();

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateEarningsDisplay() {

        DecimalFormat df = new DecimalFormat("#,##0.00");

        tvAvailableBalance.setText("Rs. " + df.format(availableBalance));
        tvTotalEarnings.setText("Rs. " + df.format(totalEarnings));

        btnRequestPayout.setEnabled(availableBalance > 0);
    }

    private void updateStatsDisplay() {

        DecimalFormat df = new DecimalFormat("#,##0.0");

        tvTotalRides.setText(String.valueOf(totalRides));
        tvTotalDistance.setText(df.format(totalDistance) + " km");
    }

    private void updateEmptyState() {

        if (payoutList.isEmpty()) {

            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);

        } else {

            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(Payout payout, int position) {
        showPayoutDetailsDialog(payout);
    }

    @Override
    public void onWithdrawClick(Payout payout, int position) {

        if (!"available".equals(payout.getStatus())) {
            showError("This payout is not available for withdrawal");
            return;
        }

        Toast.makeText(this, "Withdraw clicked", Toast.LENGTH_SHORT).show();
    }

    private void showPayoutDetailsDialog(Payout payout) {

        new AlertDialog.Builder(this)
                .setTitle("Payout Details")
                .setMessage(getPayoutDetailsMessage(payout))
                .setPositiveButton("OK", null)
                .show();
    }

    private String getPayoutDetailsMessage(Payout payout) {

        StringBuilder message = new StringBuilder();

        message.append("Period: ").append(payout.getPeriod()).append("\n");
        message.append("Amount: Rs. ").append(payout.getAmount()).append("\n");
        message.append("Rides: ").append(payout.getRideCount()).append("\n");
        message.append("Status: ").append(payout.getStatus()).append("\n");

        if (payout.getRequestedAt() > 0) {

            SimpleDateFormat sdf =
                    new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

            message.append("Requested: ")
                    .append(sdf.format(new Date(payout.getRequestedAt())));
        }

        return message.toString();
    }

    private void showLoading(boolean show) {

        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        btnRequestPayout.setEnabled(!show && availableBalance > 0);
    }

    private void showError(String message) {

        Snackbar.make(mainLayout, message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}