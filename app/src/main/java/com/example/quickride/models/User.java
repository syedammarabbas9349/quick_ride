package com.example.quickride.models;

import androidx.annotation.Keep;

import com.google.firebase.database.DataSnapshot;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Unified User model for both Customers and Drivers
 *
 * Supports:
 * - Common fields for all users
 * - Customer-specific fields
 * - Driver-specific fields
 * - Rating system
 */
@Keep
public class User {

    // Common fields for all users
    private String id = "";
    private String name = "";
    private String email = "";
    private String phone = "";
    private String profileImageUrl = "default";
    private String userType = ""; // "customer" or "driver"
    private long createdAt = 0;
    private String notificationKey = "";

    // Rating fields
    private double rating = 5.0;
    private int totalRatings = 0;
    private double ratingSum = 0;

    // Driver-specific fields
    private String car = "";
    private String carModel = "";
    private String licensePlate = "";
    private String vehicleType = ""; // "economy", "premium", "xl", "bike", "rickshaw"
    private boolean isOnline = false;
    private boolean isActive = true; // Admin approval for drivers
    private String currentRideId = "";

    // Customer-specific fields
    private String favoriteLocations = "";
    private String paymentMethodDefault = "";

    /**
     * Empty constructor required for Firebase
     */
    public User() {}

    /**
     * Constructor with ID
     */
    public User(String id) {
        this.id = id;
    }

    /**
     * Constructor for new user
     */
    public User(String id, String name, String email, String userType) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.userType = userType;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Parse DataSnapshot into this object
     * Works for both Customers and Drivers
     */
    public void parseData(DataSnapshot dataSnapshot) {
        if (dataSnapshot == null) return;

        this.id = dataSnapshot.getKey() != null ? dataSnapshot.getKey() : "";

        // Common fields
        if (dataSnapshot.child("name").getValue() != null) {
            this.name = dataSnapshot.child("name").getValue().toString();
        }
        if (dataSnapshot.child("email").getValue() != null) {
            this.email = dataSnapshot.child("email").getValue().toString();
        }
        if (dataSnapshot.child("phone").getValue() != null) {
            this.phone = dataSnapshot.child("phone").getValue().toString();
        }
        if (dataSnapshot.child("profileImageUrl").getValue() != null) {
            this.profileImageUrl = dataSnapshot.child("profileImageUrl").getValue().toString();
        }
        if (dataSnapshot.child("notificationKey").getValue() != null) {
            this.notificationKey = dataSnapshot.child("notificationKey").getValue().toString();
        }
        if (dataSnapshot.child("createdAt").getValue() != null) {
            this.createdAt = dataSnapshot.child("createdAt").getValue(Long.class);
        }

        // Rating fields
        parseRating(dataSnapshot);

        // Driver-specific fields
        if (dataSnapshot.child("car").getValue() != null) {
            this.car = dataSnapshot.child("car").getValue().toString();
        }
        if (dataSnapshot.child("carModel").getValue() != null) {
            this.carModel = dataSnapshot.child("carModel").getValue().toString();
        }
        if (dataSnapshot.child("licensePlate").getValue() != null) {
            this.licensePlate = dataSnapshot.child("licensePlate").getValue().toString();
        }
        if (dataSnapshot.child("vehicleType").getValue() != null) {
            this.vehicleType = dataSnapshot.child("vehicleType").getValue().toString();
        }
        if (dataSnapshot.child("isOnline").getValue() != null) {
            this.isOnline = dataSnapshot.child("isOnline").getValue(Boolean.class);
        }
        if (dataSnapshot.child("isActive").getValue() != null) {
            this.isActive = dataSnapshot.child("isActive").getValue(Boolean.class);
        }
        if (dataSnapshot.child("currentRideId").getValue() != null) {
            this.currentRideId = dataSnapshot.child("currentRideId").getValue().toString();
        }
    }

    /**
     * Parse rating data from snapshot
     */
    private void parseRating(DataSnapshot dataSnapshot) {
        // Try to get average rating directly
        if (dataSnapshot.child("rating").getValue() != null) {
            try {
                this.rating = dataSnapshot.child("rating").getValue(Double.class);
            } catch (Exception e) {
                // If it's not a double, might be stored differently
            }
        }

        // Calculate from rating children if available
        if (dataSnapshot.child("rating").hasChildren()) {
            double sum = 0;
            int count = 0;

            for (DataSnapshot ratingSnapshot : dataSnapshot.child("rating").getChildren()) {
                try {
                    double value = Double.parseDouble(ratingSnapshot.getValue().toString());
                    sum += value;
                    count++;
                } catch (Exception e) {
                    // Skip invalid ratings
                }
            }

            if (count > 0) {
                this.rating = sum / count;
                this.totalRatings = count;
                this.ratingSum = sum;
            }
        }

        // Try individual rating fields
        if (dataSnapshot.child("totalRatings").getValue() != null) {
            this.totalRatings = dataSnapshot.child("totalRatings").getValue(Integer.class);
        }
        if (dataSnapshot.child("ratingSum").getValue() != null) {
            this.ratingSum = dataSnapshot.child("ratingSum").getValue(Double.class);
        }
    }

    /**
     * Convert to Map for Firebase update
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("name", name);
        map.put("email", email);
        map.put("phone", phone);
        map.put("profileImageUrl", profileImageUrl);
        map.put("userType", userType);
        map.put("createdAt", createdAt);
        map.put("rating", rating);
        map.put("totalRatings", totalRatings);
        map.put("ratingSum", ratingSum);

        // Driver fields
        if ("driver".equals(userType)) {
            map.put("car", car);
            map.put("carModel", carModel);
            map.put("licensePlate", licensePlate);
            map.put("vehicleType", vehicleType);
            map.put("isOnline", isOnline);
            map.put("isActive", isActive);
            map.put("currentRideId", currentRideId);
        }

        // Customer fields
        if ("customer".equals(userType)) {
            map.put("favoriteLocations", favoriteLocations);
            map.put("paymentMethodDefault", paymentMethodDefault);
        }

        return map;
    }

    // ==================== GETTERS & SETTERS ====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameDash() {
        return name.isEmpty() ? "—" : name;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public boolean hasProfileImage() {
        return profileImageUrl != null && !profileImageUrl.equals("default") && !profileImageUrl.isEmpty();
    }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public boolean isCustomer() {
        return "customer".equals(userType);
    }

    public boolean isDriver() {
        return "driver".equals(userType);
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getNotificationKey() { return notificationKey; }
    public void setNotificationKey(String notificationKey) { this.notificationKey = notificationKey; }

    // ==================== RATING METHODS ====================

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getTotalRatings() { return totalRatings; }
    public void setTotalRatings(int totalRatings) { this.totalRatings = totalRatings; }

    public double getRatingSum() { return ratingSum; }
    public void setRatingSum(double ratingSum) { this.ratingSum = ratingSum; }

    public String getRatingString() {
        DecimalFormat df = new DecimalFormat("#.#");
        return df.format(rating);
    }

    public String getRatingStarString() {
        return getRatingString() + " ★";
    }

    /**
     * Add a new rating and recalculate average
     */
    public void addRating(double newRating) {
        this.ratingSum += newRating;
        this.totalRatings++;
        this.rating = ratingSum / totalRatings;
    }

    // ==================== DRIVER SPECIFIC METHODS ====================

    public String getCar() { return car; }
    public void setCar(String car) { this.car = car; }

    public String getCarDash() {
        return car.isEmpty() ? "—" : car;
    }

    public String getCarModel() { return carModel; }
    public void setCarModel(String carModel) { this.carModel = carModel; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getCurrentRideId() { return currentRideId; }
    public void setCurrentRideId(String currentRideId) { this.currentRideId = currentRideId; }

    /**
     * Get full car info for display
     */
    public String getCarInfo() {
        StringBuilder info = new StringBuilder();
        if (!car.isEmpty()) info.append(car);
        if (!carModel.isEmpty()) {
            if (info.length() > 0) info.append(" • ");
            info.append(carModel);
        }
        if (!licensePlate.isEmpty()) {
            if (info.length() > 0) info.append(" • ");
            info.append(licensePlate);
        }
        return info.toString();
    }

    // ==================== CUSTOMER SPECIFIC METHODS ====================

    public String getFavoriteLocations() { return favoriteLocations; }
    public void setFavoriteLocations(String favoriteLocations) { this.favoriteLocations = favoriteLocations; }

    public String getPaymentMethodDefault() { return paymentMethodDefault; }
    public void setPaymentMethodDefault(String paymentMethodDefault) { this.paymentMethodDefault = paymentMethodDefault; }

    // ==================== UTILITY METHODS ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        User user = (User) obj;
        return id != null ? id.equals(user.id) : user.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", userType='" + userType + '\'' +
                ", rating=" + rating +
                '}';
    }
}