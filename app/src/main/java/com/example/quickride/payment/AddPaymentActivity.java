package com.example.quickride.payment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Activity for adding payment methods
 * Supports: JazzCash, EasyPaisa, Cash
 */
public class AddPaymentActivity extends AppCompatActivity {

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
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        paymentMethodsRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child("Customers")
                .child(currentUserId)
                .child("paymentMethods");
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> savePaymentMethod());
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

    private String getPaymentTypeName(String type) {
        switch (type) {
            case "jazzcash": return "JazzCash";
            case "easypaisa": return "EasyPaisa";
            case "cash": return "Cash";
            default: return type;
        }
    }

    private boolean validateInputs(String type, String mobileNumber, String accountHolder) {
        if (!type.equals("cash")) {
            // Validate mobile number
            if (TextUtils.isEmpty(mobileNumber)) {
                etMobileNumber.setError("Mobile number required");
                return false;
            }
            if (!isValidPakistaniMobile(mobileNumber)) {
                etMobileNumber.setError("Invalid Pakistani mobile number");
                return false;
            }

            // Validate account holder
            if (TextUtils.isEmpty(accountHolder)) {
                etAccountHolder.setError("Account holder name required");
                return false;
            }
        }
        return true;
    }

    private boolean isValidPakistaniMobile(String number) {
        // Pakistani mobile number validation
        // Format: 03XXXXXXXXX or 3XXXXXXXXX
        String cleaned = number.replaceAll("[\\s-]", "");
        return cleaned.matches("^(03|3)\\d{9}$");
    }

    private String formatMobileNumber(String number) {
        String cleaned = number.replaceAll("[\\s-]", "");
        if (cleaned.length() == 10) {
            return "0" + cleaned.substring(0, 3) + "-" + cleaned.substring(3, 6) + "-" + cleaned.substring(6);
        } else if (cleaned.length() == 11) {
            return cleaned.substring(0, 4) + "-" + cleaned.substring(4, 7) + "-" + cleaned.substring(7);
        }
        return number;
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

        // Create payment method
        String paymentId = UUID.randomUUID().toString();
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setId(paymentId);
        paymentMethod.setType(type);
        paymentMethod.setName(getPaymentTypeName(type));

        if (!type.equals("cash")) {
            paymentMethod.setMobileNumber(mobileNumber);
            paymentMethod.setAccountHolderName(accountHolder);

            // Format mobile number for display
            String formattedNumber = formatMobileNumber(mobileNumber);
            paymentMethod.setDetails(formattedNumber);
        } else {
            paymentMethod.setDetails("Pay with cash at dropoff");
        }

        // Check if this is the first payment method
        checkAndSetDefault(paymentId, paymentMethod);
    }

    private void checkAndSetDefault(String paymentId, PaymentMethod paymentMethod) {
        // Check if there are any existing payment methods
        paymentMethodsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean hasExisting = task.getResult().exists() && task.getResult().getChildrenCount() > 0;
                paymentMethod.setDefault(!hasExisting); // Set as default if first payment method
                saveToFirebase(paymentId, paymentMethod);
            } else {
                saveToFirebase(paymentId, paymentMethod);
            }
        });
    }

    private void saveToFirebase(String paymentId, PaymentMethod paymentMethod) {
        Map<String, Object> paymentMap = new HashMap<>();
        paymentMap.put("id", paymentMethod.getId());
        paymentMap.put("type", paymentMethod.getType());
        paymentMap.put("name", paymentMethod.getName());
        paymentMap.put("mobileNumber", paymentMethod.getMobileNumber());
        paymentMap.put("accountHolderName", paymentMethod.getAccountHolderName());
        paymentMap.put("details", paymentMethod.getDetails());
        paymentMap.put("isDefault", paymentMethod.isDefault());
        paymentMap.put("addedAt", System.currentTimeMillis());

        paymentMethodsRef.child(paymentId).setValue(paymentMap)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(AddPaymentActivity.this,
                            "Payment method added successfully", Toast.LENGTH_SHORT).show();

                    // Return result
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("paymentMethod", paymentId);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(AddPaymentActivity.this,
                            "Error adding payment: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnSave.setEnabled(!show);
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