package com.example.quickride.payment;
import androidx.annotation.NonNull;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.quickride.R;
import com.example.quickride.models.PaymentMethod;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AddPaymentActivity extends AppCompatActivity {

    private static final String TAG = "AddPaymentActivity";

    // UI Components
    private Toolbar toolbar;
    private RadioGroup radioGroup;
    private RadioButton rbJazzCash, rbEasyPaisa, rbCash;
    private EditText etMobileNumber, etAccountHolder;
    private Button btnSave;
    private ProgressBar progressBar;

    // Firebase
    private DatabaseReference paymentMethodsRef;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_payment);

        initializeViews();
        setupToolbar();
        setupFirebase();
        setupListeners();
        setupPaymentTypeListener();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        radioGroup = findViewById(R.id.paymentMethodGroup);
        rbJazzCash = findViewById(R.id.rbJazzCash);
        rbEasyPaisa = findViewById(R.id.rbEasyPaisa);
        rbCash = findViewById(R.id.rbCash);
        etMobileNumber = findViewById(R.id.etMobileNumber);
        etAccountHolder = findViewById(R.id.etAccountHolder);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        // Set default selection
        rbJazzCash.setChecked(true);
        updateFieldsForPaymentType("jazzcash");
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.add_payment_method);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupFirebase() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
            Log.d(TAG, "User ID: " + currentUserId);

            paymentMethodsRef = FirebaseDatabase.getInstance()
                    .getReference()
                    .child("Users")
                    .child("Customers")
                    .child(currentUserId)
                    .child("paymentMethods");

            Log.d(TAG, "Database path: " + paymentMethodsRef.toString());
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> savePaymentMethod());
    }

    private void setupPaymentTypeListener() {
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbJazzCash) {
                updateFieldsForPaymentType("jazzcash");
            } else if (checkedId == R.id.rbEasyPaisa) {
                updateFieldsForPaymentType("easypaisa");
            } else if (checkedId == R.id.rbCash) {
                updateFieldsForPaymentType("cash");
            }
        });
    }

    private void updateFieldsForPaymentType(String type) {
        if (type.equals("cash")) {
            etMobileNumber.setVisibility(View.GONE);
            etAccountHolder.setVisibility(View.GONE);
            findViewById(R.id.tvMobileNumber).setVisibility(View.GONE);
            findViewById(R.id.tvAccountHolder).setVisibility(View.GONE);
        } else {
            etMobileNumber.setVisibility(View.VISIBLE);
            etAccountHolder.setVisibility(View.VISIBLE);
            findViewById(R.id.tvMobileNumber).setVisibility(View.VISIBLE);
            findViewById(R.id.tvAccountHolder).setVisibility(View.VISIBLE);

            if (type.equals("jazzcash")) {
                etMobileNumber.setHint("03XXXXXXXXX");
            } else if (type.equals("easypaisa")) {
                etMobileNumber.setHint("03XXXXXXXXX");
            }
        }
    }

    private String getSelectedPaymentType() {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.rbJazzCash) {
            return "jazzcash";
        } else if (selectedId == R.id.rbEasyPaisa) {
            return "easypaisa";
        } else {
            return "cash";
        }
    }

    private boolean validateInputs(String type, String mobileNumber, String accountHolder) {
        if (!type.equals("cash")) {
            if (TextUtils.isEmpty(mobileNumber)) {
                etMobileNumber.setError("Mobile number required");
                return false;
            }
            if (mobileNumber.length() < 10) {
                etMobileNumber.setError("Enter valid mobile number");
                return false;
            }
            if (TextUtils.isEmpty(accountHolder)) {
                etAccountHolder.setError("Account holder name required");
                return false;
            }
        }
        return true;
    }

    private void savePaymentMethod() {
        String type = getSelectedPaymentType();
        String mobileNumber = etMobileNumber.getText().toString().trim();
        String accountHolder = etAccountHolder.getText().toString().trim();

        // Validate
        if (!validateInputs(type, mobileNumber, accountHolder)) {
            return;
        }

        // Show loading
        showLoading(true);

        // Generate a unique key
        String paymentId = paymentMethodsRef.push().getKey();
        if (paymentId == null) {
            showLoading(false);
            Toast.makeText(this, "Error generating payment ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Saving payment method with ID: " + paymentId);

        // Create PaymentMethod object
        PaymentMethod paymentMethod;
        if (type.equals("cash")) {
            paymentMethod = new PaymentMethod(paymentId);
        } else {
            paymentMethod = new PaymentMethod(paymentId, type, mobileNumber, accountHolder);
        }

        // Check if this is the first payment method to set as default
        checkAndSetDefault(paymentId, paymentMethod);
    }

    private void checkAndSetDefault(String paymentId, PaymentMethod paymentMethod) {
        paymentMethodsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean hasExisting = snapshot.exists() && snapshot.getChildrenCount() > 0;
                paymentMethod.setDefault(!hasExisting);

                // Now save to Firebase
                saveToFirebase(paymentId, paymentMethod);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error checking existing payments: " + error.getMessage());
                // Still try to save even if check fails
                saveToFirebase(paymentId, paymentMethod);
            }
        });
    }

    private void saveToFirebase(String paymentId, PaymentMethod paymentMethod) {
        Log.d(TAG, "Saving to Firebase: " + paymentMethod.toString());

        paymentMethodsRef.child(paymentId).setValue(paymentMethod)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(AddPaymentActivity.this,
                            "Payment method added successfully", Toast.LENGTH_SHORT).show();

                    Log.d(TAG, "Payment saved successfully at: " +
                            paymentMethodsRef.child(paymentId).toString());

                    // Return result
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("paymentMethod", paymentId);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error saving payment: " + e.getMessage(), e);
                    Toast.makeText(AddPaymentActivity.this,
                            "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnSave.setEnabled(!show);
        btnSave.setText(show ? "Saving..." : "Save Payment Method");

        etMobileNumber.setEnabled(!show);
        etAccountHolder.setEnabled(!show);
        rbJazzCash.setEnabled(!show);
        rbEasyPaisa.setEnabled(!show);
        rbCash.setEnabled(!show);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}