package com.example.quickride.utils;

import android.content.Context;

import com.example.quickride.BuildConfig;
import com.example.quickride.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Helper class for sending push notifications using OneSignal
 */
public class NotificationHelper {

    private static NotificationHelper instance;
    private Context context;
    private boolean isInitialized = false;

    private NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized NotificationHelper getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationHelper(context);
        }
        return instance;
    }

    public void initialize() {
        if (isInitialized) return;

        // OneSignal v5 initialization - app ID passed directly
        OneSignal.initWithContext(context, context.getString(R.string.onesignal_app_id));

        if (BuildConfig.DEBUG) {
            OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (userId != null) {
            // In v5, use login() instead of setExternalUserId()
            OneSignal.login(userId);
        }

        // Email is handled through tags in v5
        String email = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getEmail() : null;
        if (email != null) {
            // Use setEmail()
            OneSignal.User.setEmail(email);
        }

        isInitialized = true;
    }

    public void initialize(String userType, String userId) {
        initialize();
        setUserTags(userType, userId);
        savePlayerIdToFirebase(userType, userId);
    }

    public void setUserTags(String userType, String userId) {
        // In v5, tags are set via the user object
        OneSignal.User.addTag("user_type", userType);
        OneSignal.User.addTag("user_id", userId);
        OneSignal.User.addTag("app_version", BuildConfig.VERSION_NAME);
    }

    private void savePlayerIdToFirebase(String userType, String userId) {
        // In v5, get device state
        String playerId = OneSignal.getDeviceState().getUserId();
        if (playerId != null) {
            FirebaseDatabase.getInstance()
                    .getReference()
                    .child("Users")
                    .child(userType)
                    .child(userId)
                    .child("notificationKey")
                    .setValue(playerId);
        }
    }

    /**
     * Send notification to a specific user
     */
    public void sendNotification(String playerId, String heading, String message) {
        if (playerId == null || playerId.isEmpty()) return;

        try {
            JSONObject notificationContent = new JSONObject();
            notificationContent.put("contents", new JSONObject().put("en", message));
            notificationContent.put("headings", new JSONObject().put("en", heading));

            JSONArray playerIds = new JSONArray();
            playerIds.put(playerId);
            notificationContent.put("include_player_ids", playerIds);

            JSONObject data = new JSONObject();
            data.put("type", "general");
            data.put("timestamp", System.currentTimeMillis());
            notificationContent.put("data", data);

            OneSignal.postNotification(notificationContent, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send notification with custom data
     */
    public void sendNotification(String playerId, String heading, String message, JSONObject additionalData) {
        if (playerId == null || playerId.isEmpty()) return;

        try {
            JSONObject notificationContent = new JSONObject();
            notificationContent.put("contents", new JSONObject().put("en", message));
            notificationContent.put("headings", new JSONObject().put("en", heading));

            JSONArray playerIds = new JSONArray();
            playerIds.put(playerId);
            notificationContent.put("include_player_ids", playerIds);

            if (additionalData != null) {
                notificationContent.put("data", additionalData);
            }

            OneSignal.postNotification(notificationContent, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send notification to multiple users
     */
    public void sendBulkNotification(List<String> playerIds, String heading, String message) {
        if (playerIds == null || playerIds.isEmpty()) return;

        try {
            JSONObject notificationContent = new JSONObject();
            notificationContent.put("contents", new JSONObject().put("en", message));
            notificationContent.put("headings", new JSONObject().put("en", heading));

            JSONArray idsArray = new JSONArray();
            for (String id : playerIds) {
                idsArray.put(id);
            }
            notificationContent.put("include_player_ids", idsArray);

            OneSignal.postNotification(notificationContent, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ==================== RIDE NOTIFICATIONS ====================

    public void sendRideRequestNotification(List<String> driverPlayerIds,
                                            String customerName,
                                            String pickupLocation,
                                            String rideId) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "ride_request");
            data.put("rideId", rideId);
            data.put("customerName", customerName);
            data.put("pickupLocation", pickupLocation);
            data.put("timestamp", System.currentTimeMillis());

            for (String playerId : driverPlayerIds) {
                sendNotification(playerId, "New Ride Request",
                        customerName + " needs a ride from " + pickupLocation, data);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendRideAcceptedNotification(String customerPlayerId,
                                             String driverName,
                                             String driverCar,
                                             String rideId) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "ride_accepted");
            data.put("rideId", rideId);
            data.put("driverName", driverName);
            data.put("driverCar", driverCar);
            data.put("timestamp", System.currentTimeMillis());

            sendNotification(customerPlayerId, "Ride Accepted",
                    driverName + " is on the way with " + driverCar, data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendDriverArrivedNotification(String customerPlayerId,
                                              String driverName,
                                              String rideId) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "driver_arrived");
            data.put("rideId", rideId);
            data.put("timestamp", System.currentTimeMillis());

            sendNotification(customerPlayerId, "Driver Arrived",
                    driverName + " has arrived at your location", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendRideStartedNotification(String customerPlayerId,
                                            String driverName,
                                            String rideId) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "ride_started");
            data.put("rideId", rideId);
            data.put("timestamp", System.currentTimeMillis());

            sendNotification(customerPlayerId, "Ride Started",
                    "Your ride with " + driverName + " has started", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendRideCompletedNotification(String customerPlayerId,
                                              double fare,
                                              String rideId) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "ride_completed");
            data.put("rideId", rideId);
            data.put("fare", fare);
            data.put("timestamp", System.currentTimeMillis());

            String message = String.format("Your ride is complete. Total: Rs. %.0f", fare);
            sendNotification(customerPlayerId, "Ride Completed", message, data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendRideCancelledNotification(String recipientPlayerId,
                                              String cancelledBy,
                                              String reason,
                                              String rideId) {
        try {
            JSONObject data = new JSONObject();
            data.put("type", "ride_cancelled");
            data.put("rideId", rideId);
            data.put("cancelledBy", cancelledBy);
            data.put("reason", reason);
            data.put("timestamp", System.currentTimeMillis());

            String title = "Ride Cancelled";
            String message = "Your ride has been cancelled" +
                    (reason != null ? ": " + reason : "");

            sendNotification(recipientPlayerId, title, message, data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ==================== STATIC UTILITY METHODS ====================

    /**
     * Static method for backward compatibility
     */
    public static void send(String playerId, String heading, String message) {
        if (instance != null) {
            instance.sendNotification(playerId, heading, message);
        }
    }

    // ==================== UTILITY METHODS ====================

    public void playNotificationSound() {
        // This is handled by OneSignal automatically
    }

    public void logout() {
        OneSignal.logout();
    }

    public boolean areNotificationsEnabled() {
        return OneSignal.getDeviceState() != null &&
                OneSignal.getDeviceState().isNotificationEnabled();
    }

    public void promptForPushNotifications() {
        OneSignal.getNotifications().requestPermission(false, null);
    }
}