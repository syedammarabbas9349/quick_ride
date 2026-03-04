package com.example.quickride.driver;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.quickride.R;
import com.example.quickride.adapters.PayoutAdapter;
import com.example.quickride.models.Payout;
import com.example.quickride.payment.AddPaymentActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DriverSettingsActivity extends AppCompatActivity {
    private EditText mNameField, mPhoneField, mCarField;
    private Button mBack, mConfirm, mChooseType;
    private ImageView mProfileImage;
    private ProgressBar mProgressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference mDriverDatabase;
    private DatabaseReference mPayoutDatabase;

    private String userID;
    private String mName, mPhone, mCar;
    private Uri resultUri;

    private RecyclerView payoutRecyclerView;
    private PayoutAdapter payoutAdapter;
    private List<Payout> payoutList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_settings);

        initializeViews();
        setupFirebase();
        getUserInfo();
        setupPayoutRecyclerView();
        loadPayoutHistoryFromFirebase();

        mProfileImage.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 1);
        });

        mConfirm.setOnClickListener(view -> saveUserInfo());
        mBack.setOnClickListener(view -> finish());

        mChooseType.setOnClickListener(v -> {
            Intent intent = new Intent(DriverSettingsActivity.this, DriverChooseTypeActivity.class);
            startActivity(intent);
        });
    }

    private void initializeViews() {
        mNameField = findViewById(R.id.name);
        mPhoneField = findViewById(R.id.phone);
        mCarField = findViewById(R.id.car);
        mProfileImage = findViewById(R.id.profileImage);
        mBack = findViewById(R.id.back);
        mConfirm = findViewById(R.id.confirm);
        mChooseType = findViewById(R.id.chooseType);
        mProgressBar = findViewById(R.id.progressBar);
        payoutRecyclerView = findViewById(R.id.payoutRecyclerView);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userID = mAuth.getCurrentUser().getUid();
        mDriverDatabase = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(userID);
        mPayoutDatabase = FirebaseDatabase.getInstance().getReference()
                .child("Payouts").child(userID);
    }

    private void getUserInfo() {
        mProgressBar.setVisibility(View.VISIBLE);

        mDriverDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mProgressBar.setVisibility(View.GONE);

                if (dataSnapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if (map.get("name") != null)
                        mNameField.setText(map.get("name").toString());
                    if (map.get("phone") != null)
                        mPhoneField.setText(map.get("phone").toString());
                    if (map.get("car") != null)
                        mCarField.setText(map.get("car").toString());
                    if (map.get("profileImageUrl") != null &&
                            !map.get("profileImageUrl").toString().isEmpty()) {
                        Glide.with(getApplication())
                                .load(map.get("profileImageUrl").toString())
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(mProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mProgressBar.setVisibility(View.GONE);
                Toast.makeText(DriverSettingsActivity.this,
                        "Error loading profile: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserInfo() {
        mName = mNameField.getText().toString().trim();
        mPhone = mPhoneField.getText().toString().trim();
        mCar = mCarField.getText().toString().trim();

        if (mName.isEmpty()) {
            mNameField.setError("Name is required");
            return;
        }
        if (mPhone.isEmpty()) {
            mPhoneField.setError("Phone is required");
            return;
        }
        if (mCar.isEmpty()) {
            mCarField.setError("Car details are required");
            return;
        }

        mProgressBar.setVisibility(View.VISIBLE);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("name", mName);
        userInfo.put("phone", mPhone);
        userInfo.put("car", mCar);

        mDriverDatabase.updateChildren(userInfo)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(DriverSettingsActivity.this,
                            "Profile Updated Successfully", Toast.LENGTH_SHORT).show();

                    if (resultUri != null) {
                        uploadProfileImage();
                    } else {
                        mProgressBar.setVisibility(View.GONE);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    mProgressBar.setVisibility(View.GONE);
                    Toast.makeText(DriverSettingsActivity.this,
                            "Update Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadProfileImage() {
        StorageReference filePath = FirebaseStorage.getInstance().getReference()
                .child("profile_images").child(userID);

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                    getApplication().getContentResolver(), resultUri);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);
            byte[] data = baos.toByteArray();

            UploadTask uploadTask = filePath.putBytes(data);

            uploadTask.addOnSuccessListener(taskSnapshot ->
                            filePath.getDownloadUrl().addOnSuccessListener(uri -> {
                                Map<String, Object> newImage = new HashMap<>();
                                newImage.put("profileImageUrl", uri.toString());

                                mDriverDatabase.updateChildren(newImage)
                                        .addOnCompleteListener(task -> {
                                            mProgressBar.setVisibility(View.GONE);
                                            finish();
                                        });
                            }))
                    .addOnFailureListener(e -> {
                        mProgressBar.setVisibility(View.GONE);
                        Toast.makeText(DriverSettingsActivity.this,
                                "Image upload failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        finish();
                    });

        } catch (IOException e) {
            e.printStackTrace();
            mProgressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupPayoutRecyclerView() {
        RecyclerView payoutRecyclerView = findViewById(R.id.payoutRecyclerView);
        List<Payout> payoutList = new ArrayList<>();

        // Sample data - replace with Firebase data
        Payout payout1 = new Payout();
        payout1.setPeriod("Week 12, 2024");
        payout1.setAmount(12500.0);
        payout1.setRideCount(25);
        payout1.setStatus("available");
        payout1.setRequestedAt(System.currentTimeMillis() - 86400000);
        payoutList.add(payout1);

        Payout payout2 = new Payout();
        payout2.setPeriod("Week 11, 2024");
        payout2.setAmount(10800.0);
        payout2.setRideCount(22);
        payout2.setStatus("pending");
        payout2.setRequestedAt(System.currentTimeMillis() - 172800000);
        payoutList.add(payout2);

        Payout payout3 = new Payout();
        payout3.setPeriod("Week 10, 2024");
        payout3.setAmount(9500.0);
        payout3.setRideCount(19);
        payout3.setStatus("paid");
        payout3.setRequestedAt(System.currentTimeMillis() - 604800000);
        payoutList.add(payout3);

        PayoutAdapter adapter = new PayoutAdapter(payoutList, this, new PayoutAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Payout payout, int position) {
                // Handle item click
                showPayoutDetails(payout);
            }

            @Override
            public void onWithdrawClick(Payout payout, int position) {
                // Handle withdraw button click
                showWithdrawDialog(payout, position);
            }
        });

        payoutRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        payoutRecyclerView.setAdapter(adapter);
    }

    private void showPayoutDetails(Payout payout) {
        new AlertDialog.Builder(this)
                .setTitle("Payout Details")
                .setMessage(
                        "Period: " + payout.getPeriod() + "\n" +
                                "Amount: Rs. " + payout.getAmount() + "\n" +
                                "Rides: " + payout.getRideCount() + "\n" +
                                "Status: " + payout.getStatus()
                )
                .setPositiveButton("OK", null)
                .show();
    }

    private void showWithdrawDialog(Payout payout, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Withdraw Earnings")
                .setMessage("Request withdrawal of Rs. " + payout.getAmount() + "?")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    // Update payout status in Firebase
                    Toast.makeText(this, "Withdrawal requested for Rs. " + payout.getAmount(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadPayoutHistoryFromFirebase() {
        mProgressBar.setVisibility(View.VISIBLE);

        // Query payouts ordered by period (newest first)
        Query query = mPayoutDatabase.orderByChild("period").limitToLast(20);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                payoutList.clear();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Payout payout = dataSnapshot.getValue(Payout.class);
                    if (payout != null) {
                        payout.setPayoutId(dataSnapshot.getKey());
                        payoutList.add(0, payout); // Add at beginning for reverse order
                    }
                }

                payoutAdapter.notifyDataSetChanged();
                mProgressBar.setVisibility(View.GONE);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                mProgressBar.setVisibility(View.GONE);
                Toast.makeText(DriverSettingsActivity.this,
                        "Error loading payouts: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPayoutDetailsDialog(Payout payout) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

        new AlertDialog.Builder(this)
                .setTitle("Payout Details")
                .setMessage(
                        "Period: " + payout.getPeriod() + "\n" +
                                "Amount: Rs. " + payout.getAmount() + "\n" +
                                "Rides: " + payout.getRideCount() + "\n" +
                                "Status: " + payout.getStatus() + "\n" +
                                (payout.getTransactionId() != null && !payout.getTransactionId().isEmpty() ?
                                        "Transaction ID: " + payout.getTransactionId() + "\n" : "") +
                                (payout.getProcessedAt() > 0 ?
                                        "Processed: " + sdf.format(new Date(payout.getProcessedAt())) : "")
                )
                .setPositiveButton("OK", null)
                .show();
    }

    private void checkPaymentMethods(Payout payout) {
        DatabaseReference paymentMethodsRef = FirebaseDatabase.getInstance().getReference()
                .child("Users").child("Drivers").child(userID).child("paymentMethods");

        paymentMethodsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    // User has payment methods, proceed with withdrawal
                    processWithdrawal(payout);
                } else {
                    // No payment methods, prompt to add one
                    new AlertDialog.Builder(DriverSettingsActivity.this)
                            .setTitle("No Payment Method")
                            .setMessage("Please add a payment method (JazzCash/EasyPaisa) before withdrawing.")
                            .setPositiveButton("Add Now", (dialog, which) -> {
                                // Navigate to add payment activity
                                Intent intent = new Intent(DriverSettingsActivity.this,
                                        AddPaymentActivity.class);
                                startActivity(intent);
                            })
                            .setNegativeButton("Later", null)
                            .show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DriverSettingsActivity.this,
                        "Error checking payment methods: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processWithdrawal(Payout payout) {
        new AlertDialog.Builder(DriverSettingsActivity.this)
                .setTitle("Confirm Withdrawal")
                .setMessage("Request withdrawal of Rs. " + payout.getAmount() + "?\n\n" +
                        "This will be processed within 2-3 business days.")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    mProgressBar.setVisibility(View.VISIBLE);

                    // Update payout status in Firebase
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "pending");
                    updates.put("requestedAt", System.currentTimeMillis());

                    mPayoutDatabase.child(payout.getPayoutId()).updateChildren(updates)
                            .addOnSuccessListener(aVoid -> {
                                mProgressBar.setVisibility(View.GONE);
                                Toast.makeText(DriverSettingsActivity.this,
                                        "Withdrawal request submitted successfully",
                                        Toast.LENGTH_LONG).show();

                                // Update local list - find and update the specific payout
                                for (int i = 0; i < payoutList.size(); i++) {
                                    if (payoutList.get(i).getPayoutId().equals(payout.getPayoutId())) {
                                        payoutList.get(i).setStatus("pending");
                                        payoutList.get(i).setRequestedAt(System.currentTimeMillis());
                                        payoutAdapter.notifyItemChanged(i);
                                        break;
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                mProgressBar.setVisibility(View.GONE);
                                Toast.makeText(DriverSettingsActivity.this,
                                        "Error: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            resultUri = data.getData();
            mProfileImage.setImageURI(resultUri);
        }
    }
}