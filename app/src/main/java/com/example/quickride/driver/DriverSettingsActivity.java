package com.example.quickride.driver;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.quickride.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DriverSettingsActivity extends AppCompatActivity {

    private EditText mNameField, mPhoneField, mCarField;
    private Button mConfirm, mChooseType;
    private ImageView mProfileImage;
    private ProgressBar mProgressBar;
    private Toolbar mToolbar;

    private DatabaseReference mDriverDatabase;
    private String userID;
    private Uri resultUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_settings);

        initializeViews();
        setupToolbar();
        setupFirebase();
        getUserInfo();

        mProfileImage.setOnClickListener(v -> selectImage());
        mConfirm.setOnClickListener(v -> saveUserInfo());

        mChooseType.setOnClickListener(v -> {
            startActivity(new Intent(this, DriverChooseTypeActivity.class));
        });
    }

    private void initializeViews() {
        mNameField = findViewById(R.id.name);
        mPhoneField = findViewById(R.id.phone);
        mCarField = findViewById(R.id.car);
        mProfileImage = findViewById(R.id.profileImage);
        mConfirm = findViewById(R.id.confirm);
        mChooseType = findViewById(R.id.chooseType);
        mProgressBar = findViewById(R.id.progressBar);
        mToolbar = findViewById(R.id.toolbar);
    }

    private void setupToolbar() {
        setSupportActionBar(mToolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mToolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupFirebase() {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        userID = auth.getCurrentUser().getUid();

        mDriverDatabase = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child("Drivers")
                .child(userID);
    }

    private void getUserInfo() {
        showLoading(true);

        mDriverDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                showLoading(false);

                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String car = snapshot.child("car").getValue(String.class);
                    String image = snapshot.child("profileImageUrl").getValue(String.class);

                    mNameField.setText(name != null ? name : "");
                    mPhoneField.setText(phone != null ? phone : "");
                    mCarField.setText(car != null ? car : "");

                    if (image != null && !image.isEmpty()) {
                        Glide.with(DriverSettingsActivity.this)
                                .load(image)
                                .apply(RequestOptions.circleCropTransform())
                                .into(mProfileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
            }
        });
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    private void saveUserInfo() {
        String name = mNameField.getText().toString().trim();
        String phone = mPhoneField.getText().toString().trim();
        String car = mCarField.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            mNameField.setError("Required");
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            mPhoneField.setError("Required");
            return;
        }

        if (TextUtils.isEmpty(car)) {
            mCarField.setError("Required");
            return;
        }

        showLoading(true);

        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("phone", phone);
        map.put("car", car);

        mDriverDatabase.updateChildren(map)
                .addOnSuccessListener(aVoid -> {
                    if (resultUri != null) {
                        uploadProfileImage();
                    } else {
                        showLoading(false);
                        finish();
                    }
                });
    }

    private void uploadProfileImage() {
        StorageReference fileRef = FirebaseStorage.getInstance()
                .getReference()
                .child("profile_images")
                .child(userID + ".jpg");

        fileRef.putFile(resultUri)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        fileRef.getDownloadUrl().addOnCompleteListener(uriTask -> {
                            if (uriTask.isSuccessful()) {
                                Uri downloadUri = uriTask.getResult();

                                Map<String, Object> map = new HashMap<>();
                                map.put("profileImageUrl", downloadUri.toString());

                                mDriverDatabase.updateChildren(map)
                                        .addOnSuccessListener(aVoid -> {
                                            showLoading(false);
                                            Toast.makeText(DriverSettingsActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            showLoading(false);
                                            Toast.makeText(DriverSettingsActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                showLoading(false);
                                Toast.makeText(DriverSettingsActivity.this, "Failed to get download URL", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        showLoading(false);
                        Toast.makeText(DriverSettingsActivity.this, "Upload failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoading(boolean show) {
        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        mConfirm.setEnabled(!show);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            resultUri = data.getData();

            try {
                Glide.with(this)
                        .load(resultUri)
                        .circleCrop()
                        .into(mProfileImage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}