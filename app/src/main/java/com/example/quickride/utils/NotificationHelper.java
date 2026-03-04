package com.example.quickride.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.quickride.BuildConfig;
import com.example.quickride.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.jetbrains.annotations.NotNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Helper class for managing OneSignal v5 Push Notifications using REST API
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static NotificationHelper instance;
    private final Context context;
    private final OkHttpClient httpClient;
    private boolean isInitialized = false;

    private NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static synchronized NotificationHelper getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationHelper(context);
        }
        return instance;
    }

    /**
     * Standard Initialization
     */
    public void initialize() {
        if (isInitialized) return;

        try {
            // OneSignal v5 initialization
            OneSignal.initWithContext(context, context.getString(R.string.onesignal_app_id));

            if (BuildConfig.DEBUG) {
                OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
            }

            // User Login (v5 style)
            String firebaseUid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                    FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

            if (firebaseUid != null) {
                OneSignal.login(firebaseUid);
                Log.d(TAG, "User logged in to OneSignal: " + firebaseUid);
            }

            isInitialized = true;
            Log.d(TAG, "OneSignal initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing OneSignal", e);
        }
    }

    /**
     * Initialization with User Metadata
     */
    public void initialize(String userType, String userId) {
        initialize();
        setUserTags(userType, userId);
        syncSubscriptionIdToFirebase(userType, userId);
    }

    public void setUserTags(String userType, String userId) {
        try {
            OneSignal.getUser().addTag("user_type", userType);
            OneSignal.getUser().addTag("user_id", userId);
            OneSignal.getUser().addTag("app_version", BuildConfig.VERSION_NAME);
            Log.d(TAG, "User tags set: " + userType + ", " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Error setting user tags", e);
        }
    }

    /**
     * Sync subscription ID to Firebase
     */
    private void syncSubscriptionIdToFirebase(String userType, String userId) {
        try {
            String subscriptionId = OneSignal.getUser().getPushSubscription().getId();

            if (subscriptionId != null && !subscriptionId.isEmpty()) {
                FirebaseDatabase.getInstance()
                        .getReference()
                        .child("Users")
                        .child(userType)
                        .child(userId)
                        .child("notificationKey")
                        .setValue(subscriptionId)
                        .addOnSuccessListener(aVoid ->
                                Log.d(TAG, "Subscription ID saved to Firebase"))
                        .addOnFailureListener(e ->
                                Log.e(TAG, "Firebase Update Failed", e));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing subscription ID", e);
        }
    }

    /**
     * Send notification via OneSignal REST API
     * @param subscriptionIds List of subscription IDs to send to
     * @param heading Notification title
     * @param message Notification body
     * @param additionalData Additional data payload
     */
    public void sendNotification(List<String> subscriptionIds, String heading, String message, JSONObject additionalData) {
        if (subscriptionIds == null || subscriptionIds.isEmpty()) {
            Log.w(TAG, "No subscription IDs provided");
            return;
        }

        // Get the OneSignal App ID and REST API Key from strings.xml
        String appId = context.getString(R.string.onesignal_app_id);
        String restApiKey = context.getString(R.string.onesignal_rest_api_key);

        if (appId.isEmpty() || restApiKey.isEmpty()) {
            Log.e(TAG, "OneSignal App ID or REST API Key not configured");
            return;
        }

        try {
            JSONObject notificationContent = new JSONObject();

            // App ID
            notificationContent.put("app_id", appId);

            // Content & Headings
            JSONObject contents = new JSONObject().put("en", message);
            JSONObject headings = new JSONObject().put("en", heading);

            notificationContent.put("contents", contents);
            notificationContent.put("headings", headings);

            // Target specific devices via Subscription IDs (use include_subscription_ids in v5)
            notificationContent.put("include_subscription_ids", new JSONArray(subscriptionIds));

            // Add additional data if provided
            if (additionalData != null && additionalData.length() > 0) {
                notificationContent.put("data", additionalData);
            }

            Log.d(TAG, "Sending notification: " + notificationContent.toString());

            // Build REST API request
            Request request = new Request.Builder()
                    .url("https://onesignal.com/api/v1/notifications")
                    .post(RequestBody.create(notificationContent.toString(), JSON))
                    .addHeader("Authorization", "Basic " + restApiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            // Execute async request
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Notification failed: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Notification sent successfully: " + responseBody);
                        } else {
                            Log.e(TAG, "Notification failed with code " + response.code() + ": " + responseBody);
                        }
                    } finally {
                        response.close();
                    }
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "JSON Construction Error", e);
        }
    }

    /**
     * Convenience method for sending to a single recipient
     */
    public void sendNotification(String subscriptionId, String heading, String message, JSONObject additionalData) {
        if (subscriptionId == null || subscriptionId.isEmpty()) return;

        List<String> ids = java.util.Collections.singletonList(subscriptionId);
        sendNotification(ids, heading, message, additionalData);
    }

    // ==================== RIDE NOTIFICATIONS ====================

    public void sendRideRequestNotification(List<String> driverIds, String customerName, String pickup, String rideId) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "ride_request");
            data.put("rideId", rideId);
            data.put("customerName", customerName);
            data.put("pickupLocation", pickup);
            data.put("timestamp", System.currentTimeMillis());

            sendNotification(driverIds, "New Ride Request",
                    customerName + " needs a ride from " + pickup, data);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating ride request notification", e);
        }
    }

    public void sendRideAcceptedNotification(String customerId, String driverName, String car, String rideId) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "ride_accepted");
            data.put("rideId", rideId);
            data.put("driverName", driverName);
            data.put("driverCar", car);
            data.put("timestamp", System.currentTimeMillis());

            sendNotification(customerId, "Ride Accepted",
                    driverName + " is arriving in a " + car, data);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating ride accepted notification", e);
        }
    }

    public void sendDriverArrivedNotification(String customerId, String driverName, String rideId) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "driver_arrived");
            data.put("rideId", rideId);
            data.put("driverName", driverName);
            data.put("timestamp", System.currentTimeMillis());

            sendNotification(customerId, "Driver Arrived",
                    driverName + " has arrived at your location", data);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating driver arrived notification", e);
        }
    }

    public void sendRideStartedNotification(String customerId, String driverName, String rideId) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "ride_started");
            data.put("rideId", rideId);
            data.put("driverName", driverName);
            data.put("timestamp", System.currentTimeMillis());

            sendNotification(customerId, "Ride Started",
                    "Your ride with " + driverName + " has started", data);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating ride started notification", e);
        }
    }

    public void sendRideCompletedNotification(String customerId, double fare, String rideId) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "ride_completed");
            data.put("rideId", rideId);
            data.put("fare", fare);
            data.put("timestamp", System.currentTimeMillis());

            String message = String.format("Your ride is complete. Total: Rs. %.0f", fare);
            sendNotification(customerId, "Ride Completed", message, data);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating ride completed notification", e);
        }
    }

    public void sendRideCancelledNotification(String recipientId, String cancelledBy, String reason, String rideId) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "ride_cancelled");
            data.put("rideId", rideId);
            data.put("cancelledBy", cancelledBy);
            data.put("reason", reason);
            data.put("timestamp", System.currentTimeMillis());

            String title = "Ride Cancelled";
            String message = "Your ride has been cancelled" +
                    (reason != null && !reason.isEmpty() ? ": " + reason : "");

            sendNotification(recipientId, title, message, data);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating ride cancelled notification", e);
        }
    }

    // ==================== UTILITY METHODS ====================

    public void logout() {
        try {
            OneSignal.logout();
            Log.d(TAG, "User logged out from OneSignal");
        } catch (Exception e) {
            Log.e(TAG, "Error during logout", e);
        }
    }

    public boolean isPushEnabled() {
        try {
            return OneSignal.getUser().getPushSubscription().getOptedIn();
        } catch (Exception e) {
            Log.e(TAG, "Error checking push enabled status", e);
            return false;
        }
    }

    /**
     * Prompt user for notification permission - FIXED VERSION with Continuation
     */
    public void promptPermission() {
        try {
            // In OneSignal v5, requestPermission requires a Continuation callback
            OneSignal.getNotifications().requestPermission(true, new Continuation<Boolean>() {
                @NotNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NotNull Object result) {
                    if (result instanceof Boolean) {
                        boolean granted = (Boolean) result;
                        Log.d(TAG, "Permission " + (granted ? "granted" : "denied"));
                    } else if (result instanceof Throwable) {
                        Log.e(TAG, "Permission error", (Throwable) result);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error prompting permission", e);
        }
    }

    /**
     * Get current subscription ID
     */
    public String getSubscriptionId() {
        try {
            return OneSignal.getUser().getPushSubscription().getId();
        } catch (Exception e) {
            Log.e(TAG, "Error getting subscription ID", e);
            return null;
        }
    }
}