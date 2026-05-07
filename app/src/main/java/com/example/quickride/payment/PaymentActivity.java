package com.example.quickride.payment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.cardview.widget.CardView;
import android.widget.Button;

import com.example.quickride.R;
import com.example.quickride.adapters.CardAdapter;
import com.example.quickride.models.PaymentMethod;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for managing payment methods.
 * Phase 1: Cash on Delivery + QuickRide Wallet toggle.
 * Saved methods (JazzCash / EasyPaisa) listed below as future placeholders.
 */
public class PaymentActivity extends AppCompatActivity {

    // UI Components
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private ImageView btnAddCard;
    private TextView tvEmptyState;
    private CircularProgressIndicator progressBar;
    private CardView cashCard;

    // ── Payment Mode toggle ──────────────────────────────────────────────
    private LinearLayout cashOptionRow, jazzcashOptionRow, easypaisaOptionRow;
    private RadioButton radioCash, radioJazzCash, radioEasyPaisa;

    // Data
    private List<PaymentMethod> paymentMethods = new ArrayList<>();
    private CardAdapter cardAdapter;
    private DatabaseReference paymentMethodsRef;
    private DatabaseReference userPrefRef;
    private String currentUserId;

    // Current selected mode: "cash" or "wallet"
    private String selectedMode = "cash";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        initializeViews();
        setupToolbar();
        setupFirebase();
        setupPaymentToggle();
        setupRecyclerView();
        setupClickListeners();
        loadSavedPreference();
        loadPaymentMethods();
    }

    private void initializeViews() {
        toolbar          = findViewById(R.id.toolbar);
        recyclerView     = findViewById(R.id.recyclerView);
        btnAddCard       = findViewById(R.id.add_card_image);
        tvEmptyState     = findViewById(R.id.tvEmptyState);
        progressBar      = findViewById(R.id.progressBar);
        cashCard         = findViewById(R.id.cashCard);

        // Toggle
        cashOptionRow      = findViewById(R.id.cashOptionRow);
        jazzcashOptionRow  = findViewById(R.id.jazzcashOptionRow);
        easypaisaOptionRow = findViewById(R.id.easypaisaOptionRow);
        radioCash          = findViewById(R.id.radioCash);
        radioJazzCash      = findViewById(R.id.radioJazzCash);
        radioEasyPaisa     = findViewById(R.id.radioEasyPaisa);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.payment_methods);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupFirebase() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            finish();
            return;
        }
        paymentMethodsRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child("Customers")
                .child(currentUserId)
                .child("paymentMethods");

        userPrefRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child("Customers")
                .child(currentUserId);
    }

    // ── Payment Toggle (Cash / Wallet) ────────────────────────────────────

    private void setupPaymentToggle() {
        cashOptionRow.setOnClickListener(v -> selectPaymentMode("cash"));
        jazzcashOptionRow.setOnClickListener(v -> selectPaymentMode("jazzcash"));
        easypaisaOptionRow.setOnClickListener(v -> selectPaymentMode("easypaisa"));
        
        radioCash.setOnClickListener(v -> selectPaymentMode("cash"));
        radioJazzCash.setOnClickListener(v -> selectPaymentMode("jazzcash"));
        radioEasyPaisa.setOnClickListener(v -> selectPaymentMode("easypaisa"));
    }

    private void selectPaymentMode(String mode) {
        selectedMode = mode;

        radioCash.setChecked("cash".equals(mode));
        radioJazzCash.setChecked("jazzcash".equals(mode));
        radioEasyPaisa.setChecked("easypaisa".equals(mode));

        savePaymentPreference(mode);
        
        String toastMsg = mode.substring(0, 1).toUpperCase() + mode.substring(1) + " selected ✓";
        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
    }

    private void savePaymentPreference(String mode) {
        if (userPrefRef == null) return;
        // Crash-safe: Network timeouts or permission errors are silently swallowed
        try {
            userPrefRef.child("defaultPaymentMode").setValue(mode)
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Could not save preference. Check connection.",
                                    Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            // Guard against API timeout / unavailable service
        }
    }

    private void loadSavedPreference() {
        if (userPrefRef == null) return;
        userPrefRef.child("defaultPaymentMode")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String pref = snapshot.getValue(String.class);
                            if (pref != null) {
                                selectPaymentMode(pref);
                            } else {
                                selectPaymentMode("cash");
                            }
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError error) { /* silent */ }
                });
    }


    // ── Saved Payment Methods ─────────────────────────────────────────────

    private void setupRecyclerView() {
        cardAdapter = new CardAdapter(paymentMethods, this, new CardAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(PaymentMethod paymentMethod, int position) {
                showPaymentMethodDialog(paymentMethod, position);
            }
            @Override
            public void onSetDefault(PaymentMethod paymentMethod, int position) {
                setAsDefault(paymentMethod, position);
            }
            @Override
            public void onDelete(PaymentMethod paymentMethod, int position) {
                showDeleteConfirmation(paymentMethod, position);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(cardAdapter);
    }

    private void setupClickListeners() {
        if (btnAddCard != null) {
            btnAddCard.setOnClickListener(v ->
                    Toast.makeText(this,
                            "Card/online payment integration coming soon. Use Cash or Wallet for now.",
                            Toast.LENGTH_LONG).show());
        }

        if (cashCard != null) {
            cashCard.setOnClickListener(v -> {
                PaymentMethod cash = findCashMethod();
                if (cash != null) {
                    showPaymentMethodDialog(cash, paymentMethods.indexOf(cash));
                }
            });
        }
    }

    private void loadPaymentMethods() {
        showLoading(true);

        paymentMethodsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                paymentMethods.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    PaymentMethod method = ds.getValue(PaymentMethod.class);
                    if (method != null) {
                        method.setId(ds.getKey());
                        paymentMethods.add(method);
                    }
                }
                ensureCashMethod();
                cardAdapter.notifyDataSetChanged();
                updateEmptyState();
                showLoading(false);
            }
            @Override
            public void onCancelled(DatabaseError error) {
                showLoading(false);
            }
        });
    }

    private void ensureCashMethod() {
        for (PaymentMethod method : paymentMethods) {
            if ("cash".equals(method.getType())) return;
        }
        PaymentMethod cash = new PaymentMethod("cash");
        cash.setName("Cash");
        cash.setType("cash");
        cash.setDetails("Pay with cash at dropoff");
        cash.setDefault(paymentMethods.isEmpty());
        paymentMethods.add(0, cash);
    }

    private PaymentMethod findCashMethod() {
        for (PaymentMethod method : paymentMethods) {
            if ("cash".equals(method.getType())) return method;
        }
        return null;
    }

    private void showPaymentMethodDialog(PaymentMethod method, int position) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_payment_method, null);
        dialog.setContentView(dialogView);

        TextView tvMethodName = dialogView.findViewById(R.id.tvMethodName);
        TextView tvMethodDetails = dialogView.findViewById(R.id.tvMethodDetails);
        ImageView ivMethodIcon = dialogView.findViewById(R.id.ivMethodIcon);

        tvMethodName.setText(method.getName());

        boolean isCash = "cash".equals(method.getType());
        if (isCash) {
            tvMethodDetails.setText("Pay with cash at dropoff");
        } else {
            tvMethodDetails.setText("Coming Soon – will be available in the next update");
            tvMethodDetails.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
        ivMethodIcon.setImageResource(method.getIconResource());

        Button btnSetDefault = dialogView.findViewById(R.id.btnSetDefault);
        if (isCash) {
            btnSetDefault.setVisibility(method.isDefault() ? View.GONE : View.VISIBLE);
            btnSetDefault.setOnClickListener(v -> { setAsDefault(method, position); dialog.dismiss(); });
        } else {
            btnSetDefault.setVisibility(View.GONE);
        }

        Button btnDelete = dialogView.findViewById(R.id.btnDelete);
        if (isCash) {
            btnDelete.setVisibility(View.GONE);
        } else {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setText("Not Available");
            btnDelete.setEnabled(false);
            btnDelete.setAlpha(0.5f);
        }

        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setAsDefault(PaymentMethod method, int position) {
        showLoading(true);
        for (PaymentMethod pm : paymentMethods) {
            if (pm.isDefault()) { pm.setDefault(false); updateMethodInFirebase(pm); }
        }
        method.setDefault(true);
        updateMethodInFirebase(method);
        showLoading(false);
        cardAdapter.notifyDataSetChanged();
    }

    private void updateMethodInFirebase(PaymentMethod method) {
        if ("cash".equals(method.getType())) return;
        paymentMethodsRef.child(method.getId())
                .child("isDefault").setValue(method.isDefault());
    }

    private void showDeleteConfirmation(PaymentMethod method, int position) {
        if (!"cash".equals(method.getType())) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Not Available")
                    .setMessage("JazzCash/EasyPaisa methods cannot be deleted yet.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_payment_method)
                .setMessage(getString(R.string.delete_confirmation, method.getName()))
                .setPositiveButton(R.string.delete, (d, w) -> deletePaymentMethod(method, position))
                .setNegativeButton(R.string.cancel, null).show();
    }

    private void deletePaymentMethod(PaymentMethod method, int position) {
        if ("cash".equals(method.getType())) return;
        showLoading(true);
        paymentMethodsRef.child(method.getId()).removeValue()
                .addOnSuccessListener(v -> {
                    paymentMethods.remove(position);
                    cardAdapter.notifyDataSetChanged();
                    updateEmptyState();
                    showLoading(false);
                    if (method.isDefault()) {
                        PaymentMethod cash = findCashMethod();
                        if (cash != null) cash.setDefault(true);
                    }
                })
                .addOnFailureListener(e -> showLoading(false));
    }

    private void updateEmptyState() {
        boolean hasNonCash = false;
        for (PaymentMethod m : paymentMethods) {
            if (!"cash".equals(m.getType())) { hasNonCash = true; break; }
        }
        tvEmptyState.setVisibility(hasNonCash ? View.GONE : View.VISIBLE);
        recyclerView.setVisibility(hasNonCash ? View.VISIBLE : View.GONE);
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            loadPaymentMethods();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}