package com.example.quickride.payment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickride.R;
import com.example.quickride.adapters.PayoutAdapter;
import com.example.quickride.models.Payout;
import com.example.quickride.models.User;
import com.example.quickride.utils.PaymentUtils;
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
    private TextView tvDailyEarnings;
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
    private double dailyEarnings = 0;
    private double monthlyEarnings = 0;

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
        tvDailyEarnings = findViewById(R.id.tvDailyEarnings);
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
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        
        showLoading(true);

        driverRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    showLoading(false);
                    return;
                }

                try {
                    // Fetch from stats sub-node
                    DataSnapshot statsSnapshot = snapshot.child("stats");
                    Object ridesObj = statsSnapshot.child("totalRides").getValue();
                    totalRides = (ridesObj instanceof Number) ? ((Number) ridesObj).intValue() : 0;

                    Object distanceObj = statsSnapshot.child("totalDistance").getValue();
                    totalDistance = (distanceObj instanceof Number) ? ((Number) distanceObj).doubleValue() : 0.0;

                    updateStatsDisplay();
                } catch (Exception e) {
                    Log.e("PayoutActivity", "Error parsing driver stats", e);
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
                if (!snapshot.exists()) {
                    showLoading(false);
                    return;
                }

                try {
                    // Safe parsing for availableBalance
                    Object balanceObj = snapshot.child("available").getValue();
                    availableBalance = (balanceObj instanceof Number) ? ((Number) balanceObj).doubleValue() : 0.0;

                    // Safe parsing for totalEarnings
                    Object totalObj = snapshot.child("total").getValue();
                    totalEarnings = (totalObj instanceof Number) ? ((Number) totalObj).doubleValue() : 0.0;

                    // Safe parsing for dailyEarnings
                    Object dailyObj = snapshot.child("daily").getValue();
                    dailyEarnings = (dailyObj instanceof Number) ? ((Number) dailyObj).doubleValue() : 0.0;

                    // Safe parsing for monthlyEarnings
                    Object monthlyObj = snapshot.child("monthly").getValue();
                    monthlyEarnings = (monthlyObj instanceof Number) ? ((Number) monthlyObj).doubleValue() : 0.0;

                    updateEarningsDisplay();
                } catch (Exception e) {
                    Log.e("PayoutActivity", "Error parsing driver earnings data", e);
                }

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

        // Use PaymentUtils to select withdrawal method and enter account
        PaymentUtils.showPaymentSelectionDialog(this, (method, accountNumber) -> {
            executePayoutRequest(method, accountNumber);
        });
    }

    private void executePayoutRequest(String method, String accountNumber) {
        final double amountToWithdraw = availableBalance;
        
        // 1. Create payout record
        Map<String, Object> payout = new HashMap<>();
        payout.put("amount", amountToWithdraw);
        payout.put("status", "pending");
        payout.put("paymentMethod", method);
        payout.put("accountDetails", accountNumber != null ? accountNumber : "Cash");
        payout.put("requestedAt", System.currentTimeMillis());
        payout.put("period", "Manual Request");
        payout.put("rideCount", totalRides);

        String payoutId = payoutsRef.push().getKey();
        if (payoutId == null) return;

        payoutsRef.child(payoutId).setValue(payout).addOnSuccessListener(aVoid -> {
            // 2. Deduct from available balance using transaction
            driverRef.child("earnings").runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @NonNull
                @Override
                public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData currentData) {
                    double currentAvailable = 0;
                    if (currentData.child("available").getValue() != null) {
                        currentAvailable = Double.parseDouble(currentData.child("available").getValue().toString());
                    }
                    
                    // Reset available balance to 0 after payout request
                    currentData.child("available").setValue(0);
                    return com.google.firebase.database.Transaction.success(currentData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                    if (committed) {
                        Snackbar.make(mainLayout, "Withdrawal request for Rs. " + amountToWithdraw + " sent via " + method, Snackbar.LENGTH_LONG).show();
                        // Local update to UI
                        availableBalance = 0;
                        updateEarningsDisplay();
                    }
                }
            });
        });
    }

    private void updateEarningsDisplay() {

        DecimalFormat df = new DecimalFormat("#,##0.00");

        tvAvailableBalance.setText("Rs. " + df.format(availableBalance));
        tvTotalEarnings.setText("Rs. " + df.format(totalEarnings));
        tvDailyEarnings.setText("Rs. " + df.format(dailyEarnings));

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