package com.example.quickride.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.quickride.R;
import com.example.quickride.models.PaymentMethod;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Helper class for payment operations
 * Supports: JazzCash, EasyPaisa, Cash
 */
public class PaymentHelper {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient httpClient = new OkHttpClient();
    private static PaymentHelper instance;

    private Context context;
    private DatabaseReference paymentMethodsRef;
    private String currentUserId;

    // Callback interfaces
    public interface PaymentMethodsCallback {
        void onSuccess(List<PaymentMethod> paymentMethods);
        void onError(String error);
    }

    public interface OperationCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface PaymentInitCallback {
        void onSuccess(String paymentUrl, String transactionId);
        void onError(String error);
    }

    /**
     * Private constructor for singleton
     */
    private PaymentHelper(Context context) {
        this.context = context.getApplicationContext();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (currentUserId != null) {
            this.paymentMethodsRef = FirebaseDatabase.getInstance()
                    .getReference()
                    .child("Users")
                    .child("Customers")
                    .child(currentUserId)
                    .child("paymentMethods");
        }
    }

    /**
     * Get singleton instance
     */
    public static synchronized PaymentHelper getInstance(Context context) {
        if (instance == null) {
            instance = new PaymentHelper(context);
        }
        return instance;
    }

    // ==================== PAYMENT METHOD MANAGEMENT ====================

    /**
     * Fetch all payment methods for current user
     */
    public void fetchPaymentMethods(PaymentMethodsCallback callback) {
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }

        paymentMethodsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<PaymentMethod> methods = new ArrayList<>();

                // Add cash as default option
                PaymentMethod cash = createCashMethod();
                methods.add(cash);

                // Add saved payment methods
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    PaymentMethod method = dataSnapshot.getValue(PaymentMethod.class);
                    if (method != null) {
                        method.setId(dataSnapshot.getKey());
                        methods.add(method);

                        // If this is default, remove default from cash
                        if (method.isDefault()) {
                            cash.setDefault(false);
                        }
                    }
                }

                callback.onSuccess(methods);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    /**
     * Add a new payment method
     */
    public void addPaymentMethod(PaymentMethod method, OperationCallback callback) {
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }

        String methodId = paymentMethodsRef.push().getKey();
        if (methodId == null) {
            callback.onError("Failed to generate ID");
            return;
        }

        method.setId(methodId);
        method.setAddedAt(System.currentTimeMillis());

        // Check if this is the first method
        paymentMethodsRef.get().addOnCompleteListener(task -> {
            boolean isFirst = !task.isSuccessful() ||
                    !task.getResult().exists() ||
                    task.getResult().getChildrenCount() == 0;

            method.setDefault(isFirst);

            paymentMethodsRef.child(methodId).setValue(method)
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        });
    }

    /**
     * Update payment method
     */
    public void updatePaymentMethod(PaymentMethod method, OperationCallback callback) {
        if (currentUserId == null || method.getId() == null) {
            callback.onError("Invalid parameters");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("mobileNumber", method.getMobileNumber());
        updates.put("accountHolderName", method.getAccountHolderName());
        updates.put("isDefault", method.isDefault());

        paymentMethodsRef.child(method.getId()).updateChildren(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Set payment method as default
     */
    public void setDefaultPaymentMethod(String methodId, OperationCallback callback) {
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }

        // First, remove default from all methods
        paymentMethodsRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onError("Failed to fetch methods");
                return;
            }

            DatabaseReference updates = FirebaseDatabase.getInstance().getReference();
            Map<String, Object> childUpdates = new HashMap<>();

            for (DataSnapshot snapshot : task.getResult().getChildren()) {
                childUpdates.put(
                        "Users/Customers/" + currentUserId + "/paymentMethods/" + snapshot.getKey() + "/isDefault",
                        false
                );
            }

            // Set new default
            childUpdates.put(
                    "Users/Customers/" + currentUserId + "/paymentMethods/" + methodId + "/isDefault",
                    true
            );

            updates.updateChildren(childUpdates)
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        });
    }

    /**
     * Delete payment method
     */
    public void deletePaymentMethod(String methodId, OperationCallback callback) {
        if (currentUserId == null) {
            callback.onError("User not logged in");
            return;
        }

        paymentMethodsRef.child(methodId).removeValue()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ==================== JAZZCASH INTEGRATION ====================

    /**
     * Initialize JazzCash payment
     */
    public void initiateJazzCashPayment(String mobileNumber, double amount,
                                        String orderId, PaymentInitCallback callback) {
        // Payment gateway not yet implemented
        callback.onError("Payment gateway not yet configured. Please use Cash for now.");
    }

    // ==================== EASYPAISA INTEGRATION ====================

    /**
     * Initialize EasyPaisa payment
     */
    public void initiateEasyPaisaPayment(String mobileNumber, double amount,
                                         String orderId, PaymentInitCallback callback) {
        // Payment gateway not yet implemented
        callback.onError("Payment gateway not yet configured. Please use Cash for now.");
    }

    // ==================== PAYMENT VERIFICATION ====================

    /**
     * Verify payment status
     */
    public void verifyPayment(String transactionId, String paymentMethod,
                              OperationCallback callback) {
        String url;
        if ("jazzcash".equals(paymentMethod)) {
            url = context.getString(R.string.jazzcash_api_url) + "/verify";
        } else if ("easypaisa".equals(paymentMethod)) {
            url = context.getString(R.string.easypaisa_api_url) + "/verify";
        } else {
            callback.onError("Invalid payment method");
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("transactionId", transactionId);

        makePostRequest(url, payload, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError("Payment verification failed");
                }
            }
        });
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Create cash payment method
     */
    private PaymentMethod createCashMethod() {
        PaymentMethod cash = new PaymentMethod("cash");
        cash.setName("Cash");
        cash.setType("cash");
        cash.setDetails("Pay with cash at dropoff");
        cash.setDefault(true); // Default until another method is added
        return cash;
    }

    /**
     * Validate Pakistani mobile number
     */
    public boolean isValidPakistaniMobile(String number) {
        String cleaned = number.replaceAll("[\\s-]", "");
        return cleaned.matches("^(03|3)\\d{9}$");
    }

    /**
     * Format mobile number for display
     */
    public String formatMobileNumber(String number) {
        String cleaned = number.replaceAll("[\\s-]", "");
        if (cleaned.length() == 10) {
            return "0" + cleaned.substring(0, 3) + "-" + cleaned.substring(3, 6) + "-" + cleaned.substring(6);
        } else if (cleaned.length() == 11) {
            return cleaned.substring(0, 4) + "-" + cleaned.substring(4, 7) + "-" + cleaned.substring(7);
        }
        return number;
    }

    /**
     * Make HTTP POST request
     */
    private void makePostRequest(String url, JsonObject payload, Callback callback) {
        RequestBody body = RequestBody.create(payload.toString(), JSON_MEDIA_TYPE);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        httpClient.newCall(request).enqueue(callback);
    }

    /**
     * Open payment URL in browser
     */
    public void openPaymentUrl(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Show payment result
     */
    public void showPaymentResult(Context context, boolean success, String message) {
        Toast.makeText(context,
                success ? "Payment successful" : "Payment failed: " + message,
                Toast.LENGTH_LONG).show();
    }

    // ==================== STATIC UTILITIES ====================

    /**
     * Initialize payment SDKs (call once in Application class)
     */
    public static void initialize(Context context) {
        // Initialize any payment SDKs if needed
        // This is a placeholder for future integration
    }

    /**
     * Get payment method icon resource ID
     */
    public static int getPaymentIcon(String type) {
        switch (type != null ? type.toLowerCase() : "") {
            case "jazzcash":
                return R.drawable.ic_jazzcash;
            case "easypaisa":
                return R.drawable.ic_easypaisa;
            case "cash":
                return R.drawable.ic_cash;
            default:
                return R.drawable.ic_credit_card;
        }
    }

    /**
     * Get payment method display name
     */
    public static String getPaymentDisplayName(String type) {
        switch (type != null ? type.toLowerCase() : "") {
            case "jazzcash":
                return "JazzCash";
            case "easypaisa":
                return "EasyPaisa";
            case "cash":
                return "Cash";
            default:
                return type;
        }
    }
}