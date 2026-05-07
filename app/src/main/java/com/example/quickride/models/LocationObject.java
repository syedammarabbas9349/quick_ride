package com.example.quickride.models;

import android.location.Location;

import androidx.annotation.Keep;

import com.google.android.gms.maps.model.LatLng;

/**
 * Location Object used for pickup, destination, and current locations
 * Enhanced with utility methods for distance calculation and formatting
 */
@Keep
public class LocationObject {

    private double latitude;
    private double longitude;
    private String address = "";
    private String shortName = "";
    private String city = "";
    private String country = "";

    // For backward compatibility with LatLng
    private transient LatLng latLng;

    /**
     * Empty constructor required for Firebase
     */
    public LocationObject() {
    }

    /**
     * Constructor with LatLng and address
     */
    public LocationObject(LatLng latLng, String address) {
        this.latitude = latLng.latitude;
        this.longitude = latLng.longitude;
        this.address = address;
        this.latLng = latLng;
        extractShortName();
    }

    /**
     * Constructor with coordinates and address
     */
    public LocationObject(double latitude, double longitude, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.latLng = new LatLng(latitude, longitude);
        extractShortName();
    }

    /**
     * Extract short name from full address
     */
    private void extractShortName() {
        if (address == null || address.isEmpty()) {
            this.shortName = "Unknown Location";
            return;
        }

        // Try to get a short, readable name
        String[] parts = address.split(",");
        if (parts.length > 0) {
            this.shortName = parts[0].trim();
        } else {
            this.shortName = address;
        }
    }

    // ==================== GETTERS & SETTERS ====================

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
        this.latLng = new LatLng(latitude, longitude);
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
        this.latLng = new LatLng(latitude, longitude);
    }

    /**
     * Get coordinates as LatLng (for Google Maps)
     */
    public LatLng getLatLng() {
        if (latLng == null) {
            latLng = new LatLng(latitude, longitude);
        }
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latitude = latLng.latitude;
        this.longitude = latLng.longitude;
        this.latLng = latLng;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
        extractShortName();
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Calculate distance to another location in meters
     */
    public float distanceTo(LocationObject other) {
        if (other == null) return 0;

        float[] results = new float[1];
        Location.distanceBetween(
                this.latitude, this.longitude,
                other.latitude, other.longitude,
                results
        );
        return results[0];
    }

    /**
     * Calculate distance to another location in kilometers
     */
    public double distanceToKm(LocationObject other) {
        return distanceTo(other) / 1000.0;
    }

    /**
     * Check if location is valid (non-zero coordinates)
     */
    public boolean isValid() {
        return latitude != 0 || longitude != 0;
    }

    /**
     * Get formatted coordinates string
     */
    public String getCoordinatesString() {
        return String.format("%.6f, %.6f", latitude, longitude);
    }

    /**
     * Get Google Maps URL for this location
     */
    public String getGoogleMapsUrl() {
        return "https://maps.google.com/?q=" + latitude + "," + longitude;
    }

    /**
     * Get Waze URL for this location
     */
    public String getWazeUrl() {
        return "https://waze.com/ul?ll=" + latitude + "," + longitude + "&navigate=yes";
    }

    /**
     * Convert to Android Location object
     */
    public Location toAndroidLocation() {
        Location location = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return location;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        LocationObject that = (LocationObject) obj;
        return Double.compare(that.latitude, latitude) == 0 &&
                Double.compare(that.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(latitude);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "LocationObject{" +
                "address='" + address + '\'' +
                ", coordinates=" + getCoordinatesString() +
                '}';
    }
}