package com.example.quickride.customer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.quickride.R;
import com.example.quickride.models.User;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Modernized Customer Settings Activity with Material Design 3
 */
public class CustomerSettingsActivity extends AppCompatActivity {

    // UI Components
    private EditText mNameField, mPhoneField;
    private ImageView mProfileImage;
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

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        mNameField = findViewById(R.id.name);
        mPhoneField = findViewById(R.id.phone);
        mProfileImage = findViewById(R.id.profileImage);
        mConfirm = findViewById(R.id.confirm);
        mToolbar = findViewById(R.id.my_toolbar);
        mProgressBar = findViewById(R.id.progressBar);
    }

    /**
     * Setup Firebase references
     */
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
                .child(userID);

        mCustomer = new User();
        mCustomer.setId(userID);
        mCustomer.setUserType("customer");
    }

    /**
     * Setup toolbar with back navigation
     */
    private void setupToolbar() {
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.settings);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        mToolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * Load current user's info from Firebase
     */
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

                // Parse user data
                String name = dataSnapshot.child("name").getValue(String.class);
                String phone = dataSnapshot.child("phone").getValue(String.class);
                String imageUrl = dataSnapshot.child("profileImageUrl").getValue(String.class);

                mNameField.setText(name != null ? name : "");
                mPhoneField.setText(phone != null ? phone : "");

                // Load profile image
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
                        "Error loading profile: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Setup click listeners for UI elements
     */
    private void setupClickListeners() {
        mProfileImage.setOnClickListener(v -> selectImage());
        mConfirm.setOnClickListener(v -> saveUserInfo());
    }

    /**
     * Open image picker
     */
    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    /**
     * Save user information to Firebase
     */
    private void saveUserInfo() {
        String name = mNameField.getText().toString().trim();
        String phone = mPhoneField.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(name)) {
            mNameField.setError("Name is required");
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            mPhoneField.setError("Phone number is required");
            return;
        }

        showLoading(true);

        // Update user info in database
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("name", name);
        userInfo.put("phone", phone);

        mCustomerDatabase.updateChildren(userInfo)
                .addOnSuccessListener(aVoid -> {
                    // If image was selected, upload it
                    if (resultUri != null) {
                        uploadProfileImage();
                    } else {
                        showLoading(false);
                        Toast.makeText(CustomerSettingsActivity.this,
                                "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(CustomerSettingsActivity.this,
                            "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Upload profile image to Firebase Storage
     */
    private void uploadProfileImage() {
        if (resultUri == null) {
            showLoading(false);
            finish();
            return;
        }

        try {
            // Compress image
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);

            // Upload to Firebase Storage
            UploadTask uploadTask = mStorageRef.putFile(resultUri);

            uploadTask.addOnSuccessListener(taskSnapshot ->
                            mStorageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                // Update database with image URL
                                Map<String, Object> imageUpdate = new HashMap<>();
                                imageUpdate.put("profileImageUrl", uri.toString());

                                mCustomerDatabase.updateChildren(imageUpdate)
                                        .addOnCompleteListener(task -> {
                                            showLoading(false);
                                            Toast.makeText(CustomerSettingsActivity.this,
                                                    "Profile updated with image", Toast.LENGTH_SHORT).show();
                                            finish();
                                        });
                            }))
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(CustomerSettingsActivity.this,
                                "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    });

        } catch (IOException e) {
            showLoading(false);
            e.printStackTrace();
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Show/hide loading indicator
     */
    private void showLoading(boolean show) {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        mConfirm.setEnabled(!show);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            resultUri = data.getData();

            try {
                // Preview selected image
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        this.getContentResolver(), resultUri);

                Glide.with(this)
                        .load(bitmap)
                        .apply(RequestOptions.circleCropTransform())
                        .into(mProfileImage);

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }
}