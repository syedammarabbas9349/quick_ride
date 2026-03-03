package com.example.quickride.payment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickride.R;
import com.example.quickride.adapters.CardAdapter;
import com.example.quickride.models.PaymentMethod;
import com.example.quickride.utils.PaymentHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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
 * Activity for managing payment methods
 * Supports: JazzCash, EasyPaisa, Cash
 */
public class PaymentActivity extends AppCompatActivity {

    // UI Components
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private ImageView btnAddCard;
    private TextView tvEmptyState;
    private CircularProgressIndicator progressBar;
    private MaterialCardView cashCard;

    // Data
    private List<PaymentMethod> paymentMethods = new ArrayList<>();
    private CardAdapter cardAdapter;
    private DatabaseReference paymentMethodsRef;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        initializeViews();
        setupToolbar();
        setupFirebase();
        setupRecyclerView();
        setupClickListeners();
        loadPaymentMethods();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
        btnAddCard = findViewById(R.id.add_card_image);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        progressBar = findViewById(R.id.progressBar);
        cashCard = findViewById(R.id.cashCard);
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
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        paymentMethodsRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child("Customers")
                .child(currentUserId)
                .child("paymentMethods");
    }

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
        btnAddCard.setOnClickListener(v -> {
            Intent intent = new Intent(PaymentActivity.this, AddPaymentActivity.class);
            startActivityForResult(intent, 100);
        });

        cashCard.setOnClickListener(v -> {
            // Handle cash selection
            PaymentMethod cash = findCashMethod();
            if (cash != null) {
                showPaymentMethodDialog(cash, paymentMethods.indexOf(cash));
            }
        });
    }

    private void loadPaymentMethods() {
        showLoading(true);

        paymentMethodsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                paymentMethods.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    PaymentMethod method = dataSnapshot.getValue(PaymentMethod.class);
                    if (method != null) {
                        method.setId(dataSnapshot.getKey());
                        paymentMethods.add(method);
                    }
                }

                // Ensure cash is always available
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
        // Check if cash already exists
        for (PaymentMethod method : paymentMethods) {
            if ("cash".equals(method.getType())) {
                return;
            }
        }

        // Add cash as default option
        PaymentMethod cash = new PaymentMethod("cash");
        cash.setName("Cash");
        cash.setType("cash");
        cash.setDetails("Pay with cash at dropoff");
        cash.setDefault(paymentMethods.isEmpty()); // Set as default if no other methods
        paymentMethods.add(0, cash);
    }

    private PaymentMethod findCashMethod() {
        for (PaymentMethod method : paymentMethods) {
            if ("cash".equals(method.getType())) {
                return method;
            }
        }
        return null;
    }

    private void showPaymentMethodDialog(PaymentMethod method, int position) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_payment_method, null);
        dialog.setContentView(dialogView);

        // Set method details
        TextView tvMethodName = dialogView.findViewById(R.id.tvMethodName);
        TextView tvMethodDetails = dialogView.findViewById(R.id.tvMethodDetails);
        ImageView ivMethodIcon = dialogView.findViewById(R.id.ivMethodIcon);

        tvMethodName.setText(method.getName());
        tvMethodDetails.setText(method.getDetails());
        ivMethodIcon.setImageResource(method.getIconResource());

        // Set as default button (hide for cash or if already default)
        MaterialButton btnSetDefault = dialogView.findViewById(R.id.btnSetDefault);
        if (method.isDefault() || "cash".equals(method.getType())) {
            btnSetDefault.setVisibility(View.GONE);
        } else {
            btnSetDefault.setVisibility(View.VISIBLE);
            btnSetDefault.setOnClickListener(v -> {
                setAsDefault(method, position);
                dialog.dismiss();
            });
        }

        // Delete button (hide for cash)
        MaterialButton btnDelete = dialogView.findViewById(R.id.btnDelete);
        if ("cash".equals(method.getType())) {
            btnDelete.setVisibility(View.GONE);
        } else {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> {
                showDeleteConfirmation(method, position);
                dialog.dismiss();
            });
        }

        // Cancel button
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void setAsDefault(PaymentMethod method, int position) {
        showLoading(true);

        // Remove default from all methods
        for (PaymentMethod pm : paymentMethods) {
            if (pm.isDefault()) {
                pm.setDefault(false);
                updateMethodInFirebase(pm);
            }
        }

        // Set new default
        method.setDefault(true);
        updateMethodInFirebase(method);

        showLoading(false);
        cardAdapter.notifyDataSetChanged();
    }

    private void updateMethodInFirebase(PaymentMethod method) {
        if ("cash".equals(method.getType())) return;

        paymentMethodsRef.child(method.getId())
                .child("isDefault")
                .setValue(method.isDefault());
    }

    private void showDeleteConfirmation(PaymentMethod method, int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_payment_method)
                .setMessage(getString(R.string.delete_confirmation, method.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> deletePaymentMethod(method, position))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deletePaymentMethod(PaymentMethod method, int position) {
        if ("cash".equals(method.getType())) return;

        showLoading(true);

        paymentMethodsRef.child(method.getId())
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    paymentMethods.remove(position);
                    cardAdapter.notifyDataSetChanged();
                    updateEmptyState();
                    showLoading(false);

                    // If deleted method was default, set cash as default
                    if (method.isDefault()) {
                        PaymentMethod cash = findCashMethod();
                        if (cash != null) {
                            cash.setDefault(true);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                });
    }

    private void updateEmptyState() {
        boolean hasNonCashMethods = false;
        for (PaymentMethod method : paymentMethods) {
            if (!"cash".equals(method.getType())) {
                hasNonCashMethods = true;
                break;
            }
        }

        if (hasNonCashMethods) {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
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
            // New payment method added, refresh list
            loadPaymentMethods();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}