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

    private String userID;
    private String mName, mPhone, mCar;
    private Uri resultUri;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_settings);

        initializeViews();
        setupFirebase();
        getUserInfo();

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            resultUri = data.getData();
            mProfileImage.setImageURI(resultUri);
        }
    }
}