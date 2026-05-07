package com.example.quickride.models;

import androidx.annotation.Keep;
import com.google.android.gms.maps.model.LatLng;

@Keep
public class SharedPassenger {
    private String userId;
    private String name;
    private String phone;
    private String profileImageUrl;
    private double pickupLat;
    private double pickupLng;
    private String pickupAddress;
    private double dropoffLat;
    private double dropoffLng;
    private String dropoffAddress;
    private double fareShare;
    private long joinedAt;
    private String status; // "pending", "onboard", "dropped"
    private boolean sharingEnabled;

    public SharedPassenger() {}

    public SharedPassenger(String userId, String name, String phone,
                           String profileImageUrl, boolean sharingEnabled) {
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.profileImageUrl = profileImageUrl;
        this.sharingEnabled = sharingEnabled;
        this.joinedAt = System.currentTimeMillis();
        this.status = "pending";
    }

    // Getters
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public double getPickupLat() { return pickupLat; }
    public double getPickupLng() { return pickupLng; }
    public String getPickupAddress() { return pickupAddress; }
    public double getDropoffLat() { return dropoffLat; }
    public double getDropoffLng() { return dropoffLng; }
    public String getDropoffAddress() { return dropoffAddress; }
    public double getFareShare() { return fareShare; }
    public long getJoinedAt() { return joinedAt; }
    public String getStatus() { return status; }
    public boolean isSharingEnabled() { return sharingEnabled; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setPickupLat(double pickupLat) { this.pickupLat = pickupLat; }
    public void setPickupLng(double pickupLng) { this.pickupLng = pickupLng; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }
    public void setDropoffLat(double dropoffLat) { this.dropoffLat = dropoffLat; }
    public void setDropoffLng(double dropoffLng) { this.dropoffLng = dropoffLng; }
    public void setDropoffAddress(String dropoffAddress) { this.dropoffAddress = dropoffAddress; }
    public void setFareShare(double fareShare) { this.fareShare = fareShare; }
    public void setJoinedAt(long joinedAt) { this.joinedAt = joinedAt; }
    public void setStatus(String status) { this.status = status; }
    public void setSharingEnabled(boolean sharingEnabled) { this.sharingEnabled = sharingEnabled; }

    public LatLng getPickupLatLng() {
        return new LatLng(pickupLat, pickupLng);
    }

    public LatLng getDropoffLatLng() {
        return new LatLng(dropoffLat, dropoffLng);
    }
}