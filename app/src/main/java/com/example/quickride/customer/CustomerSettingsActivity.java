package com.example.quickride.customer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.quickride.R;
import com.example.quickride.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CustomerSettingsActivity extends AppCompatActivity {

    // UI Components
    private EditText mNameField, mPhoneField, mEmailField;
    private ImageView mProfileImage, mEditIcon;
    private TextView tvMemberSince;
    private Button mConfirm;
    private Toolbar mToolbar;
    private ProgressBar mProgressBar;

    // Firebase
    private DatabaseReference mCustomerDatabase;
    private StorageReference mStorageRef;
    private String userID;
    private Uri resultUri;
    private User mCustomer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_settings);

        initializeViews();
        setupFirebase();
        setupToolbar();
        loadUserInfo();
        setupClickListeners();
    }

    private void initializeViews() {
        mNameField = findViewById(R.id.name);
        mPhoneField = findViewById(R.id.phone);
        mEmailField = findViewById(R.id.email);
        mProfileImage = findViewById(R.id.profileImage);
        mEditIcon = findViewById(R.id.editIcon);
        tvMemberSince = findViewById(R.id.tvMemberSince);
        mConfirm = findViewById(R.id.confirm);
        mToolbar = findViewById(R.id.my_toolbar);
        mProgressBar = findViewById(R.id.progressBar);
    }

    private void setupFirebase() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userID = mAuth.getCurrentUser().getUid();

        mCustomerDatabase = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child("Customers")
                .child(userID);

        mStorageRef = FirebaseStorage.getInstance()
                .getReference()
                .child("profile_images")
                .child(userID + ".jpg");

        mCustomer = new User();
        mCustomer.setId(userID);
        mCustomer.setUserType("customer");
    }

    private void setupToolbar() {
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.settings);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        mToolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadUserInfo() {
        showLoading(true);

        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                showLoading(false);

                if (!dataSnapshot.exists()) {
                    Toast.makeText(CustomerSettingsActivity.this,
                            "User data not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                String name = dataSnapshot.child("name").getValue(String.class);
                String phone = dataSnapshot.child("phone").getValue(String.class);
                String email = dataSnapshot.child("email").getValue(String.class); // ✅ FIXED
                String imageUrl = dataSnapshot.child("profileImageUrl").getValue(String.class);

                mNameField.setText(name != null ? name : "");
                mPhoneField.setText(phone != null ? phone : "");
                mEmailField.setText(email != null ? email : ""); // ✅ FIXED

                // Member since
                Long createdAt = dataSnapshot.child("createdAt").getValue(Long.class);
                if (createdAt != null && tvMemberSince != null) {
                    String formatted = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            .format(new Date(createdAt));
                    tvMemberSince.setText("Member since: " + formatted);
                }

                // Profile Image
                if (imageUrl != null && !imageUrl.equals("default") && !imageUrl.isEmpty()) {
                    Glide.with(CustomerSettingsActivity.this)
                            .load(imageUrl)
                            .apply(RequestOptions.circleCropTransform())
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .into(mProfileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showLoading(false);
                Toast.makeText(CustomerSettingsActivity.this,
                        "Error: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        mProfileImage.setOnClickListener(v -> selectImage());

        // ✅ FIXED: Edit icon now works
        if (mEditIcon != null) {
            mEditIcon.setOnClickListener(v -> selectImage());
        }

        mConfirm.setOnClickListener(v -> saveUserInfo());
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    private void saveUserInfo() {
        String name = mNameField.getText().toString().trim();
        String phone = mPhoneField.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            mNameField.setError("Name is required");
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            mPhoneField.setError("Phone required");
            return;
        }

        // ✅ FIXED: Phone validation
        if (!phone.matches("^(03|3)\\d{9}$")) {
            mPhoneField.setError("Invalid Pakistani number");
            return;
        }

        showLoading(true);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("name", name);
        userInfo.put("phone", phone);

        mCustomerDatabase.updateChildren(userInfo)
                .addOnSuccessListener(aVoid -> {
                    if (resultUri != null) {
                        uploadProfileImage();
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadProfileImage() {
        if (resultUri == null) {
            showLoading(false);
            return;
        }

        StorageReference fileRef = FirebaseStorage.getInstance()
                .getReference()
                .child("profile_images")
                .child(userID + ".jpg");

        fileRef.putFile(resultUri)
                .addOnSuccessListener(taskSnapshot ->
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {

                            Map<String, Object> map = new HashMap<>();
                            map.put("profileImageUrl", uri.toString());

                            mCustomerDatabase.updateChildren(map)
                                    .addOnSuccessListener(aVoid -> {
                                        showLoading(false);
                                        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                                    });

                        })
                )
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        mConfirm.setEnabled(!show);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            resultUri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        this.getContentResolver(), resultUri);

                Glide.with(this)
                        .load(bitmap)
                        .apply(RequestOptions.circleCropTransform())
                        .into(mProfileImage);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}