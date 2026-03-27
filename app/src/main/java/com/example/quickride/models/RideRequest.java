package com.example.quickride.models;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.location.Location;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Keep;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.quickride.R;
import com.example.quickride.utils.NotificationHelper;
import com.example.quickride.utils.PriceCalculator;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ride Request model for both customers and drivers
 * Manages the entire lifecycle of a ride from request to completion
 */
@Keep
public class RideRequest implements Cloneable {


    private boolean sharingEnabled = false;
    private double sharingDiscount = 0.3; // 30% discount
    private double originalFare;
    private String shareRideId;
    private int maxPassengers = 4;
    private int currentPassengers = 1;
    private List<SharedPassenger> passengers = new ArrayList<>();
    private String shareStatus = "solo";
    // Ride identifiers
    private String rideId;
    private String customerId;
    private String driverId;
    private long timestamp;

    // Locations
    private LocationObject pickupLocation;
    private LocationObject destinationLocation;
    private LocationObject currentDriverLocation;

    // Ride details
    private String pickupAddress;
    private String destinationAddress;
    private double pickupLat;
    private double pickupLng;
    private double destLat;
    private double destLng;
    private double distance;           // Calculated route distance in km
    private double distanceDriven;      // Actual distance driven in km
    private long duration;              // Duration in seconds
    private double fare;                 // Final fare

    // Service details
    private String vehicleType;          // "economy", "premium", "xl"
    private String serviceType;           // "ride", "delivery"
    private double pricePerKm;
    private double baseFare;

    // Status management
    private String status;                // "pending", "accepted", "driver_arrived", "started", "completed", "cancelled"
    private int state;                     // 0-pending, 1-accepted, 2-arrived, 3-started, 4-completed, -1-cancelled

    // Timestamps
    private long createdAt;
    private long acceptedAt;
    private long arrivedAt;
    private long startedAt;
    private long completedAt;
    private long cancelledAt;

    // Cancellation info
    private boolean isCancelled;
    private String cancelledBy;            // "customer", "driver", "system"
    private String cancellationReason;
    private int cancellationType;           // 1: customer, 2: driver, 3: timeout, 4: payment

    // Payment
    private String paymentMethod;           // "jazzcash", "easypaisa", "cash"
    private boolean customerPaid;
    private boolean driverPaid;
    private String transactionId;

    // Rating
    private float rating;                    // Customer rating for driver (1-5)
    private boolean isRated;

    // Customer info (cached)
    private String customerName;
    private String customerPhone;
    private String customerImageUrl;
    private String customerNotificationKey;

    // Driver info (cached)
    private String driverName;
    private String driverPhone;
    private Double customerRating;           // Rating given by driver to customer
    private String driverImageUrl;
    private String driverCar;
    private String driverLicense;
    private String driverNotificationKey;

    // Context for notifications
    private transient Context context;

    // Database reference
    private transient DatabaseReference rideRef;
    private transient Activity activity;

    public boolean isSharingEnabled() { return sharingEnabled; }
    public void setSharingEnabled(boolean sharingEnabled) { this.sharingEnabled = sharingEnabled; }

    public double getSharingDiscount() { return sharingDiscount; }
    public void setSharingDiscount(double sharingDiscount) { this.sharingDiscount = sharingDiscount; }

    public double getOriginalFare() { return originalFare; }
    public void setOriginalFare(double originalFare) { this.originalFare = originalFare; }

    public String getShareRideId() { return shareRideId; }
    public void setShareRideId(String shareRideId) { this.shareRideId = shareRideId; }

    public int getMaxPassengers() { return maxPassengers; }
    public void setMaxPassengers(int maxPassengers) { this.maxPassengers = maxPassengers; }

    public int getCurrentPassengers() { return currentPassengers; }
    public void setCurrentPassengers(int currentPassengers) { this.currentPassengers = currentPassengers; }

    public List<SharedPassenger> getPassengers() { return passengers; }
    public void setPassengers(List<SharedPassenger> passengers) { this.passengers = passengers; }

    public String getShareStatus() { return shareStatus; }
    public void setShareStatus(String shareStatus) { this.shareStatus = shareStatus; }

    public boolean canAcceptMorePassengers() {
        return sharingEnabled && currentPassengers < maxPassengers;
    }

    public String getPassengerCountText() {
        return currentPassengers + "/" + maxPassengers + " passengers";
    }
    public RideRequest() {
    }

    /**
     * Set context for notifications
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * Get customer rating
     */
    public Double getCustomerRating() {
        return customerRating != null ? customerRating : 4.5;
    }

    /**
     * Set customer rating
     */
    public void setCustomerRating(Double customerRating) {
        this.customerRating = customerRating;
    }

    /**
     * Constructor with activity for UI operations
     */
    public RideRequest(Activity activity, String rideId) {
        this.activity = activity;
        this.rideId = rideId;
        this.context = activity;
    }

    /**
     * Constructor for new ride request
     */
    public RideRequest(String customerId, LocationObject pickup, LocationObject destination,
                       String vehicleType, double pricePerKm, double baseFare) {
        this.customerId = customerId;
        this.pickupLocation = pickup;
        this.destinationLocation = destination;
        this.pickupAddress = pickup.getAddress();
        this.destinationAddress = destination.getAddress();
        this.pickupLat = pickup.getLatitude();
        this.pickupLng = pickup.getLongitude();
        this.destLat = destination.getLatitude();
        this.destLng = destination.getLongitude();
        this.vehicleType = vehicleType;
        this.pricePerKm = pricePerKm;
        this.baseFare = baseFare;
        this.status = "pending";
        this.state = 0;
        this.createdAt = System.currentTimeMillis();

        // Calculate initial distance estimate
        calculateEstimatedDistance();
    }

    // ==================== DATABASE OPERATIONS ====================

    /**
     * Parse DataSnapshot into this object
     */
    public void parseData(DataSnapshot dataSnapshot) {
        if (dataSnapshot == null) return;

        this.rideId = dataSnapshot.getKey();

        // IDs
        if (dataSnapshot.child("customerId").getValue() != null) {
            this.customerId = dataSnapshot.child("customerId").getValue().toString();
        }
        if (dataSnapshot.child("driverId").getValue() != null) {
            this.driverId = dataSnapshot.child("driverId").getValue().toString();
        }

        // Locations
        if (dataSnapshot.child("pickupAddress").getValue() != null) {
            this.pickupAddress = dataSnapshot.child("pickupAddress").getValue().toString();
        }
        if (dataSnapshot.child("destinationAddress").getValue() != null) {
            this.destinationAddress = dataSnapshot.child("destinationAddress").getValue().toString();
        }
        if (dataSnapshot.child("pickupLat").getValue() != null) {
            this.pickupLat = dataSnapshot.child("pickupLat").getValue(Double.class);
        }
        if (dataSnapshot.child("pickupLng").getValue() != null) {
            this.pickupLng = dataSnapshot.child("pickupLng").getValue(Double.class);
        }
        if (dataSnapshot.child("destLat").getValue() != null) {
            this.destLat = dataSnapshot.child("destLat").getValue(Double.class);
        }
        if (dataSnapshot.child("destLng").getValue() != null) {
            this.destLng = dataSnapshot.child("destLng").getValue(Double.class);
        }

        // Create LocationObjects
        if (pickupLat != 0 && pickupLng != 0) {
            this.pickupLocation = new LocationObject(pickupLat, pickupLng, pickupAddress);
        }
        if (destLat != 0 && destLng != 0) {
            this.destinationLocation = new LocationObject(destLat, destLng, destinationAddress);
        }

        // Ride details
        if (dataSnapshot.child("distance").getValue() != null) {
            this.distance = dataSnapshot.child("distance").getValue(Double.class);
        }
        if (dataSnapshot.child("distanceDriven").getValue() != null) {
            this.distanceDriven = dataSnapshot.child("distanceDriven").getValue(Double.class);
        }
        if (dataSnapshot.child("duration").getValue() != null) {
            this.duration = dataSnapshot.child("duration").getValue(Long.class);
        }
        if (dataSnapshot.child("fare").getValue() != null) {
            this.fare = dataSnapshot.child("fare").getValue(Double.class);
        }

        // Service details
        if (dataSnapshot.child("vehicleType").getValue() != null) {
            this.vehicleType = dataSnapshot.child("vehicleType").getValue().toString();
        }
        if (dataSnapshot.child("serviceType").getValue() != null) {
            this.serviceType = dataSnapshot.child("serviceType").getValue().toString();
        }
        if (dataSnapshot.child("pricePerKm").getValue() != null) {
            this.pricePerKm = dataSnapshot.child("pricePerKm").getValue(Double.class);
        }
        if (dataSnapshot.child("baseFare").getValue() != null) {
            this.baseFare = dataSnapshot.child("baseFare").getValue(Double.class);
        }

        // Status
        if (dataSnapshot.child("status").getValue() != null) {
            this.status = dataSnapshot.child("status").getValue().toString();
        }
        if (dataSnapshot.child("state").getValue() != null) {
            this.state = dataSnapshot.child("state").getValue(Integer.class);
        }

        // Timestamps
        if (dataSnapshot.child("createdAt").getValue() != null) {
            this.createdAt = dataSnapshot.child("createdAt").getValue(Long.class);
        }
        if (dataSnapshot.child("acceptedAt").getValue() != null) {
            this.acceptedAt = dataSnapshot.child("acceptedAt").getValue(Long.class);
        }
        if (dataSnapshot.child("arrivedAt").getValue() != null) {
            this.arrivedAt = dataSnapshot.child("arrivedAt").getValue(Long.class);
        }
        if (dataSnapshot.child("startedAt").getValue() != null) {
            this.startedAt = dataSnapshot.child("startedAt").getValue(Long.class);
        }
        if (dataSnapshot.child("completedAt").getValue() != null) {
            this.completedAt = dataSnapshot.child("completedAt").getValue(Long.class);
        }
        if (dataSnapshot.child("cancelledAt").getValue() != null) {
            this.cancelledAt = dataSnapshot.child("cancelledAt").getValue(Long.class);
        }

        // Cancellation
        if (dataSnapshot.child("isCancelled").getValue() != null) {
            this.isCancelled = dataSnapshot.child("isCancelled").getValue(Boolean.class);
        }
        if (dataSnapshot.child("cancelledBy").getValue() != null) {
            this.cancelledBy = dataSnapshot.child("cancelledBy").getValue().toString();
        }
        if (dataSnapshot.child("cancellationReason").getValue() != null) {
            this.cancellationReason = dataSnapshot.child("cancellationReason").getValue().toString();
        }
        if (dataSnapshot.child("cancellationType").getValue() != null) {
            this.cancellationType = dataSnapshot.child("cancellationType").getValue(Integer.class);
        }

        // Payment
        if (dataSnapshot.child("paymentMethod").getValue() != null) {
            this.paymentMethod = dataSnapshot.child("paymentMethod").getValue().toString();
        }
        if (dataSnapshot.child("customerPaid").getValue() != null) {
            this.customerPaid = dataSnapshot.child("customerPaid").getValue(Boolean.class);
        }
        if (dataSnapshot.child("driverPaid").getValue() != null) {
            this.driverPaid = dataSnapshot.child("driverPaid").getValue(Boolean.class);
        }
        if (dataSnapshot.child("transactionId").getValue() != null) {
            this.transactionId = dataSnapshot.child("transactionId").getValue().toString();
        }

        // Rating
        if (dataSnapshot.child("rating").getValue() != null) {
            this.rating = dataSnapshot.child("rating").getValue(Float.class);
        }
        if (dataSnapshot.child("isRated").getValue() != null) {
            this.isRated = dataSnapshot.child("isRated").getValue(Boolean.class);
        }

        // Customer info
        if (dataSnapshot.child("customerName").getValue() != null) {
            this.customerName = dataSnapshot.child("customerName").getValue().toString();
        }
        if (dataSnapshot.child("customerPhone").getValue() != null) {
            this.customerPhone = dataSnapshot.child("customerPhone").getValue().toString();
        }
        if (dataSnapshot.child("customerImageUrl").getValue() != null) {
            this.customerImageUrl = dataSnapshot.child("customerImageUrl").getValue().toString();
        }
        if (dataSnapshot.child("customerNotificationKey").getValue() != null) {
            this.customerNotificationKey = dataSnapshot.child("customerNotificationKey").getValue().toString();
        }

        // Driver info
        if (dataSnapshot.child("driverName").getValue() != null) {
            this.driverName = dataSnapshot.child("driverName").getValue().toString();
        }
        if (dataSnapshot.child("driverPhone").getValue() != null) {
            this.driverPhone = dataSnapshot.child("driverPhone").getValue().toString();
        }
        if (dataSnapshot.child("driverImageUrl").getValue() != null) {
            this.driverImageUrl = dataSnapshot.child("driverImageUrl").getValue().toString();
        }
        if (dataSnapshot.child("driverCar").getValue() != null) {
            this.driverCar = dataSnapshot.child("driverCar").getValue().toString();
        }
        if (dataSnapshot.child("driverLicense").getValue() != null) {
            this.driverLicense = dataSnapshot.child("driverLicense").getValue().toString();
        }
        if (dataSnapshot.child("driverNotificationKey").getValue() != null) {
            this.driverNotificationKey = dataSnapshot.child("driverNotificationKey").getValue().toString();
        }

        // Set database reference
        this.rideRef = FirebaseDatabase.getInstance().getReference()
                .child("ride_info").child(this.rideId);
    }

    /**
     * Post new ride request to database
     */
    public void postRideRequest() {
        if (!validateRideRequest()) return;

        rideRef = FirebaseDatabase.getInstance().getReference().child("ride_info");
        rideId = rideRef.push().getKey();

        Map<String, Object> rideMap = new HashMap<>();

        // Basic info
        rideMap.put("customerId", customerId);
        rideMap.put("status", "pending");
        rideMap.put("state", 0);
        rideMap.put("createdAt", ServerValue.TIMESTAMP);
        rideMap.put("timestamp", System.currentTimeMillis());

        // Locations
        rideMap.put("pickupAddress", pickupAddress);
        rideMap.put("destinationAddress", destinationAddress);
        rideMap.put("pickupLat", pickupLat);
        rideMap.put("pickupLng", pickupLng);
        rideMap.put("destLat", destLat);
        rideMap.put("destLng", destLng);

        // Service details
        rideMap.put("vehicleType", vehicleType);
        rideMap.put("pricePerKm", pricePerKm);
        rideMap.put("baseFare", baseFare);
        rideMap.put("distance", distance);
        rideMap.put("fare", fare);

        // Customer info
        rideMap.put("customerName", customerName);
        rideMap.put("customerPhone", customerPhone);
        rideMap.put("customerImageUrl", customerImageUrl);
        rideMap.put("customerNotificationKey", customerNotificationKey);

        // Initial flags
        rideMap.put("isCancelled", false);
        rideMap.put("customerPaid", false);
        rideMap.put("driverPaid", false);
        rideMap.put("isRated", false);

        rideRef.child(rideId).updateChildren(rideMap);
        rideRef = rideRef.child(rideId);
    }

    /**
     * Validate ride request before posting
     */
    private boolean validateRideRequest() {
        if (activity == null) return true;

        if (pickupLocation == null) {
            Toast.makeText(activity, "Please select pickup location", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (destinationLocation == null) {
            Toast.makeText(activity, "Please select destination", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * Calculate estimated distance between pickup and destination
     */
    private void calculateEstimatedDistance() {
        if (pickupLocation == null || destinationLocation == null) return;

        float[] results = new float[1];
        Location.distanceBetween(
                pickupLat, pickupLng,
                destLat, destLng,
                results
        );
        this.distance = results[0] / 1000.0; // Convert to km
        calculateFare();
    }

    /**
     * Calculate fare based on distance and vehicle type
     */
    private void calculateFare() {
        this.fare = PriceCalculator.calculateFare(distance, pricePerKm, baseFare);
    }

    // ==================== RIDE ACTIONS ====================

    /**
     * Driver accepts the ride
     */
    public void acceptRide(String driverId, String driverName, String driverPhone,
                           String driverImage, String driverCar, String notificationKey) {
        this.driverId = driverId;
        this.driverName = driverName;
        this.driverPhone = driverPhone;
        this.driverImageUrl = driverImage;
        this.driverCar = driverCar;
        this.driverNotificationKey = notificationKey;
        this.status = "accepted";
        this.state = 1;
        this.acceptedAt = System.currentTimeMillis();

        Map<String, Object> updates = new HashMap<>();
        updates.put("driverId", driverId);
        updates.put("driverName", driverName);
        updates.put("driverPhone", driverPhone);
        updates.put("driverImageUrl", driverImage);
        updates.put("driverCar", driverCar);
        updates.put("driverNotificationKey", notificationKey);
        updates.put("status", "accepted");
        updates.put("state", 1);
        updates.put("acceptedAt", ServerValue.TIMESTAMP);

        if (rideRef != null) {
            rideRef.updateChildren(updates);
        }

        // Send notification to customer
        sendNotificationToCustomer("ride_accepted", "Driver Accepted",
                "Your driver " + driverName + " is on the way with " + driverCar);
    }

    /**
     * Driver has arrived at pickup location
     */
    public void driverArrived() {
        this.status = "driver_arrived";
        this.state = 2;
        this.arrivedAt = System.currentTimeMillis();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "driver_arrived");
        updates.put("state", 2);
        updates.put("arrivedAt", ServerValue.TIMESTAMP);

        if (rideRef != null) {
            rideRef.updateChildren(updates);
        }

        // Send notification to customer
        sendNotificationToCustomer("driver_arrived", "Driver Arrived",
                "Your driver " + driverName + " has arrived at the pickup location");
    }

    /**
     * Ride has started (customer picked up)
     */
    public void startRide() {
        this.status = "started";
        this.state = 3;
        this.startedAt = System.currentTimeMillis();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "started");
        updates.put("state", 3);
        updates.put("startedAt", ServerValue.TIMESTAMP);

        if (rideRef != null) {
            rideRef.updateChildren(updates);
        }
    }

    /**
     * Ride completed
     */
    public void completeRide() {
        this.status = "completed";
        this.state = 4;
        this.completedAt = System.currentTimeMillis();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "completed");
        updates.put("state", 4);
        updates.put("completedAt", ServerValue.TIMESTAMP);
        updates.put("distanceDriven", distanceDriven);

        if (rideRef != null) {
            rideRef.updateChildren(updates);
        }

        // Send notification to customer
        String message = String.format("Your ride is complete. Total: Rs. %.0f", fare);
        sendNotificationToCustomer("ride_completed", "Ride Completed", message);
    }

    /**
     * Cancel the ride
     */
    public void cancelRide(String cancelledBy, String reason, int type) {
        this.isCancelled = true;
        this.cancelledBy = cancelledBy;
        this.cancellationReason = reason;
        this.cancellationType = type;
        this.cancelledAt = System.currentTimeMillis();
        this.status = "cancelled";
        this.state = -1;

        Map<String, Object> updates = new HashMap<>();
        updates.put("isCancelled", true);
        updates.put("cancelledBy", cancelledBy);
        updates.put("cancellationReason", reason);
        updates.put("cancellationType", type);
        updates.put("cancelledAt", ServerValue.TIMESTAMP);
        updates.put("status", "cancelled");
        updates.put("state", -1);

        if (rideRef != null) {
            rideRef.updateChildren(updates);
        }

        // Send notification to the other party
        sendCancellationNotification(cancelledBy, reason);
    }

    /**
     * Helper method to send notification to customer
     */
    private void sendNotificationToCustomer(String type, String title, String message) {
        if (context != null && customerNotificationKey != null && !customerNotificationKey.isEmpty()) {
            List<String> recipientIds = new ArrayList<>();
            recipientIds.add(customerNotificationKey);

            try {
                JSONObject data = new JSONObject();
                data.put("type", type);
                data.put("rideId", rideId);
                if (driverName != null) data.put("driverName", driverName);
                if (fare > 0) data.put("fare", fare);

                NotificationHelper.getInstance(context).sendNotification(
                        recipientIds,
                        title,
                        message,
                        data
                );
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Helper method to send cancellation notification
     */
    private void sendCancellationNotification(String cancelledBy, String reason) {
        if (context == null) return;

        String notificationKey = cancelledBy.equals("customer") ? driverNotificationKey : customerNotificationKey;

        if (notificationKey != null && !notificationKey.isEmpty()) {
            List<String> recipientIds = new ArrayList<>();
            recipientIds.add(notificationKey);

            try {
                JSONObject data = new JSONObject();
                data.put("type", "ride_cancelled");
                data.put("rideId", rideId);
                data.put("cancelledBy", cancelledBy);
                data.put("reason", reason);

                String title = "Ride Cancelled";
                String message = "Your ride has been cancelled" +
                        (reason != null && !reason.isEmpty() ? ": " + reason : "");

                NotificationHelper.getInstance(context).sendNotification(
                        recipientIds,
                        title,
                        message,
                        data
                );
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Update driver's current location during ride
     */
    public void updateDriverLocation(double lat, double lng) {
        this.currentDriverLocation = new LocationObject(lat, lng, "");

        // Update distance driven if ride started
        if (state >= 3 && startedAt > 0) {
            // This would be handled by a separate method with previous location
        }
    }

    /**
     * Add distance to total driven distance
     */
    public void addDrivenDistance(double additionalDistance) {
        this.distanceDriven += additionalDistance;
    }

    /**
     * Submit rating for driver
     */
    public void submitRating(float rating) {
        this.rating = rating;
        this.isRated = true;

        Map<String, Object> updates = new HashMap<>();
        updates.put("rating", rating);
        updates.put("isRated", true);

        if (rideRef != null) {
            rideRef.updateChildren(updates);
        }
    }

    /**
     * Process payment
     */
    public void processPayment(String method, String transactionId) {
        this.paymentMethod = method;
        this.transactionId = transactionId;
        this.customerPaid = true;

        Map<String, Object> updates = new HashMap<>();
        updates.put("paymentMethod", method);
        updates.put("transactionId", transactionId);
        updates.put("customerPaid", true);

        if (rideRef != null) {
            rideRef.updateChildren(updates);
        }
    }

    /**
     * Mark driver as paid
     */
    public void markDriverPaid() {
        this.driverPaid = true;

        Map<String, Object> updates = new HashMap<>();
        updates.put("driverPaid", true);

        if (rideRef != null) {
            rideRef.updateChildren(updates);
        }
    }

    // ==================== UI METHODS ====================

    /**
     * Show rating dialog after ride completion
     */
    public void showRatingDialog(Activity activity) {
        if (activity == null) return;

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_ride_review);
        dialog.setCancelable(false);

        Button btnConfirm = dialog.findViewById(R.id.confirm);
        RatingBar ratingBar = dialog.findViewById(R.id.rate);
        TextView tvName = dialog.findViewById(R.id.name);
        ImageView ivImage = dialog.findViewById(R.id.image);

        tvName.setText(driverName != null ? driverName : "Driver");

        if (driverImageUrl != null && !driverImageUrl.equals("default") && !driverImageUrl.isEmpty()) {
            Glide.with(activity)
                    .load(driverImageUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(ivImage);
        }

        btnConfirm.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            if (rating > 0) {
                submitRating(rating);
                dialog.dismiss();
            } else {
                Toast.makeText(activity, "Please select a rating", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    // ==================== GETTERS & SETTERS ====================

    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public LocationObject getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(LocationObject pickupLocation) {
        this.pickupLocation = pickupLocation;
        if (pickupLocation != null) {
            this.pickupAddress = pickupLocation.getAddress();
            this.pickupLat = pickupLocation.getLatitude();
            this.pickupLng = pickupLocation.getLongitude();
        }
    }

    public LocationObject getDestinationLocation() { return destinationLocation; }
    public void setDestinationLocation(LocationObject destinationLocation) {
        this.destinationLocation = destinationLocation;
        if (destinationLocation != null) {
            this.destinationAddress = destinationLocation.getAddress();
            this.destLat = destinationLocation.getLatitude();
            this.destLng = destinationLocation.getLongitude();
        }
    }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public String getDestinationAddress() { return destinationAddress; }
    public void setDestinationAddress(String destinationAddress) { this.destinationAddress = destinationAddress; }

    public double getPickupLat() { return pickupLat; }
    public void setPickupLat(double pickupLat) { this.pickupLat = pickupLat; }

    public double getPickupLng() { return pickupLng; }
    public void setPickupLng(double pickupLng) { this.pickupLng = pickupLng; }

    public double getDestLat() { return destLat; }
    public void setDestLat(double destLat) { this.destLat = destLat; }

    public double getDestLng() { return destLng; }
    public void setDestLng(double destLng) { this.destLng = destLng; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public double getDistanceDriven() { return distanceDriven; }
    public void setDistanceDriven(double distanceDriven) { this.distanceDriven = distanceDriven; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public double getFare() { return fare; }
    public void setFare(double fare) { this.fare = fare; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public double getPricePerKm() { return pricePerKm; }
    public void setPricePerKm(double pricePerKm) { this.pricePerKm = pricePerKm; }

    public double getBaseFare() { return baseFare; }
    public void setBaseFare(double baseFare) { this.baseFare = baseFare; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getState() { return state; }
    public void setState(int state) { this.state = state; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(long acceptedAt) { this.acceptedAt = acceptedAt; }

    public long getArrivedAt() { return arrivedAt; }
    public void setArrivedAt(long arrivedAt) { this.arrivedAt = arrivedAt; }

    public long getStartedAt() { return startedAt; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }

    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public long getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(long cancelledAt) { this.cancelledAt = cancelledAt; }

    public boolean isCancelled() { return isCancelled; }
    public void setCancelled(boolean cancelled) { isCancelled = cancelled; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public int getCancellationType() { return cancellationType; }
    public void setCancellationType(int cancellationType) { this.cancellationType = cancellationType; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public boolean isCustomerPaid() { return customerPaid; }
    public void setCustomerPaid(boolean customerPaid) { this.customerPaid = customerPaid; }

    public boolean isDriverPaid() { return driverPaid; }
    public void setDriverPaid(boolean driverPaid) { this.driverPaid = driverPaid; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public boolean isRated() { return isRated; }
    public void setRated(boolean rated) { isRated = rated; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerImageUrl() { return customerImageUrl; }
    public void setCustomerImageUrl(String customerImageUrl) { this.customerImageUrl = customerImageUrl; }

    public String getCustomerNotificationKey() { return customerNotificationKey; }
    public void setCustomerNotificationKey(String customerNotificationKey) { this.customerNotificationKey = customerNotificationKey; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }

    public String getDriverImageUrl() { return driverImageUrl; }
    public void setDriverImageUrl(String driverImageUrl) { this.driverImageUrl = driverImageUrl; }

    public String getDriverCar() { return driverCar; }
    public void setDriverCar(String driverCar) { this.driverCar = driverCar; }

    public String getDriverLicense() { return driverLicense; }
    public void setDriverLicense(String driverLicense) { this.driverLicense = driverLicense; }

    public String getDriverNotificationKey() { return driverNotificationKey; }
    public void setDriverNotificationKey(String driverNotificationKey) { this.driverNotificationKey = driverNotificationKey; }

    public DatabaseReference getRideRef() { return rideRef; }

    // ==================== UTILITY METHODS ====================

    /**
     * Get formatted date string
     */
    public String getFormattedDate() {
        long time = createdAt > 0 ? createdAt : System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        return sdf.format(new Date(time));
    }

    /**
     * Get formatted fare string
     */
    public String getFormattedFare() {
        return String.format(Locale.getDefault(), "Rs. %.0f", fare);
    }

    /**
     * Get formatted distance string
     */
    public String getFormattedDistance() {
        return String.format(Locale.getDefault(), "%.1f km", distance);
    }

    /**
     * Get formatted duration string
     */
    public String getFormattedDuration() {
        long minutes = duration / 60;
        long seconds = duration % 60;

        if (minutes > 60) {
            long hours = minutes / 60;
            minutes = minutes % 60;
            return String.format(Locale.getDefault(), "%dh %dm", hours, minutes);
        } else {
            return String.format(Locale.getDefault(), "%d min", minutes);
        }
    }

    /**
     * Get driver display name with fallback
     */
    public String getDriverDisplayName() {
        return driverName != null && !driverName.isEmpty() ? driverName : "Driver";
    }

    /**
     * Get customer display name with fallback
     */
    public String getCustomerDisplayName() {
        return customerName != null && !customerName.isEmpty() ? customerName : "Customer";
    }

    @Override
    public RideRequest clone() {
        try {
            return (RideRequest) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}