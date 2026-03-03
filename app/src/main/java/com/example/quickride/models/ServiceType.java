package com.example.quickride.models;

import android.graphics.drawable.Drawable;

import androidx.annotation.Keep;
import androidx.annotation.DrawableRes;

import com.example.quickride.R;

/**
 * Service Type model for different vehicle categories
 * Supports: Economy, Premium, XL, Bike, Rickshaw
 */
@Keep
public class ServiceType {

    private String id;              // Unique identifier
    private String name;             // Display name
    private String vehicleType;      // "economy", "premium", "xl", "bike", "rickshaw"
    private String description;      // Short description

    // Pricing
    private double baseFare;         // Base fare in Rs
    private double pricePerKm;       // Rate per kilometer in Rs
    private double pricePerMinute;    // Rate per minute in Rs (for waiting/traffic)
    private double minimumFare;       // Minimum fare in Rs
    private double cancellationFee;   // Cancellation fee in Rs

    // Capacity
    private int capacity;             // Number of passengers
    private int luggageCapacity;       // Number of luggage items

    // ETA and availability
    private int estimatedMinutes;      // Estimated arrival time
    private boolean isAvailable;       // If service is currently available
    private boolean isPopular;         // If service is highlighted

    // Visuals
    @DrawableRes
    private int iconResId;             // Resource ID for vehicle icon
    @DrawableRes
    private int markerResId;           // Resource ID for map marker
    private int colorPrimary;          // Primary color for UI

    // Features
    private boolean hasAC;              // Air conditioning
    private boolean hasWiFi;            // WiFi available
    private boolean hasChildSeat;       // Child seat available
    private boolean isWheelchairAccessible; // Wheelchair accessible

    /**
     * Empty constructor required for Firebase
     */
    public ServiceType() {}

    /**
     * Constructor with basic info
     */
    public ServiceType(String id, String name, String vehicleType,
                       double pricePerKm, int capacity, @DrawableRes int iconResId) {
        this.id = id;
        this.name = name;
        this.vehicleType = vehicleType;
        this.pricePerKm = pricePerKm;
        this.capacity = capacity;
        this.iconResId = iconResId;

        // Set defaults
        this.baseFare = 50.0;
        this.minimumFare = 100.0;
        this.pricePerMinute = 2.0;
        this.cancellationFee = 50.0;
        this.isAvailable = true;
        this.estimatedMinutes = 5;

        // Set description based on vehicle type
        setDefaultDescription();

        // Set default colors
        setDefaultColors();

        // Set default features
        setDefaultFeatures();
    }

    /**
     * Full constructor
     */
    public ServiceType(String id, String name, String vehicleType, String description,
                       double baseFare, double pricePerKm, double pricePerMinute,
                       double minimumFare, int capacity, int iconResId, int colorPrimary) {
        this.id = id;
        this.name = name;
        this.vehicleType = vehicleType;
        this.description = description;
        this.baseFare = baseFare;
        this.pricePerKm = pricePerKm;
        this.pricePerMinute = pricePerMinute;
        this.minimumFare = minimumFare;
        this.capacity = capacity;
        this.iconResId = iconResId;
        this.colorPrimary = colorPrimary;
        this.isAvailable = true;
        this.estimatedMinutes = 5;

        setDefaultMarker();
    }

    private void setDefaultDescription() {
        switch (vehicleType) {
            case "economy":
                this.description = "Affordable, compact cars";
                break;
            case "premium":
                this.description = "Luxury cars, extra comfort";
                break;
            case "xl":
                this.description = "SUVs and minivans, up to 6 seats";
                break;
            case "bike":
                this.description = "Fast and affordable bike rides";
                break;
            case "rickshaw":
                this.description = "Traditional rickshaw rides";
                break;
            default:
                this.description = "Standard ride service";
        }
    }

    private void setDefaultColors() {
        switch (vehicleType) {
            case "economy":
                this.colorPrimary = 0xFF4CAF50; // Green
                break;
            case "premium":
                this.colorPrimary = 0xFF9C27B0; // Purple
                break;
            case "xl":
                this.colorPrimary = 0xFFFF5722; // Orange
                break;
            case "bike":
                this.colorPrimary = 0xFF795548; // Brown
                break;
            case "rickshaw":
                this.colorPrimary = 0xFFFF9800; // Orange
                break;
            default:
                this.colorPrimary = 0xFF2196F3; // Blue
        }
    }

    private void setDefaultMarker() {
        switch (vehicleType) {
            case "economy":
                this.markerResId = R.drawable.ic_car_marker_green;
                break;
            case "premium":
                this.markerResId = R.drawable.ic_car_marker_purple;
                break;
            case "xl":
                this.markerResId = R.drawable.ic_car_marker_orange;
                break;
            case "bike":
                this.markerResId = R.drawable.ic_bike_marker;
                break;
            case "rickshaw":
                this.markerResId = R.drawable.ic_rickshaw_marker;
                break;
            default:
                this.markerResId = R.drawable.ic_car_marker_blue;
        }
    }

    private void setDefaultFeatures() {
        switch (vehicleType) {
            case "economy":
                this.hasAC = true;
                this.hasWiFi = false;
                this.hasChildSeat = false;
                this.isWheelchairAccessible = false;
                this.luggageCapacity = 2;
                break;
            case "premium":
                this.hasAC = true;
                this.hasWiFi = true;
                this.hasChildSeat = true;
                this.isWheelchairAccessible = false;
                this.luggageCapacity = 3;
                break;
            case "xl":
                this.hasAC = true;
                this.hasWiFi = true;
                this.hasChildSeat = true;
                this.isWheelchairAccessible = true;
                this.luggageCapacity = 4;
                break;
            case "bike":
                this.hasAC = false;
                this.hasWiFi = false;
                this.hasChildSeat = false;
                this.isWheelchairAccessible = false;
                this.luggageCapacity = 0;
                break;
            case "rickshaw":
                this.hasAC = false;
                this.hasWiFi = false;
                this.hasChildSeat = false;
                this.isWheelchairAccessible = false;
                this.luggageCapacity = 1;
                break;
        }
    }

    // ==================== GETTERS & SETTERS ====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getBaseFare() { return baseFare; }
    public void setBaseFare(double baseFare) { this.baseFare = baseFare; }

    public double getPricePerKm() { return pricePerKm; }
    public void setPricePerKm(double pricePerKm) { this.pricePerKm = pricePerKm; }

    public double getPricePerMinute() { return pricePerMinute; }
    public void setPricePerMinute(double pricePerMinute) { this.pricePerMinute = pricePerMinute; }

    public double getMinimumFare() { return minimumFare; }
    public void setMinimumFare(double minimumFare) { this.minimumFare = minimumFare; }

    public double getCancellationFee() { return cancellationFee; }
    public void setCancellationFee(double cancellationFee) { this.cancellationFee = cancellationFee; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public int getLuggageCapacity() { return luggageCapacity; }
    public void setLuggageCapacity(int luggageCapacity) { this.luggageCapacity = luggageCapacity; }

    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public boolean isPopular() { return isPopular; }
    public void setPopular(boolean popular) { isPopular = popular; }

    @DrawableRes
    public int getIconResId() { return iconResId; }
    public void setIconResId(@DrawableRes int iconResId) { this.iconResId = iconResId; }

    @DrawableRes
    public int getMarkerResId() { return markerResId; }
    public void setMarkerResId(@DrawableRes int markerResId) { this.markerResId = markerResId; }

    public int getColorPrimary() { return colorPrimary; }
    public void setColorPrimary(int colorPrimary) { this.colorPrimary = colorPrimary; }

    public boolean isHasAC() { return hasAC; }
    public void setHasAC(boolean hasAC) { this.hasAC = hasAC; }

    public boolean isHasWiFi() { return hasWiFi; }
    public void setHasWiFi(boolean hasWiFi) { this.hasWiFi = hasWiFi; }

    public boolean isHasChildSeat() { return hasChildSeat; }
    public void setHasChildSeat(boolean hasChildSeat) { this.hasChildSeat = hasChildSeat; }

    public boolean isWheelchairAccessible() { return isWheelchairAccessible; }
    public void setWheelchairAccessible(boolean wheelchairAccessible) { isWheelchairAccessible = wheelchairAccessible; }

    // ==================== LEGACY GETTERS (for compatibility) ====================

    /**
     * @deprecated Use getIconResId() instead
     */
    @Deprecated
    public Drawable getImage() {
        return null; // Legacy method, use getIconResId() with Glide
    }

    /**
     * @deprecated Use getCapacity() instead
     */
    @Deprecated
    public int getPeople() {
        return capacity;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Calculate fare for a given distance
     */
    public double calculateFare(double distanceKm) {
        double fare = baseFare + (distanceKm * pricePerKm);
        return Math.max(fare, minimumFare);
    }

    /**
     * Calculate fare with duration (for traffic/waiting)
     */
    public double calculateFareWithDuration(double distanceKm, long durationMinutes) {
        double distanceFare = baseFare + (distanceKm * pricePerKm);
        double timeFare = durationMinutes * pricePerMinute;
        return Math.max(distanceFare + timeFare, minimumFare);
    }

    /**
     * Calculate fare with surge pricing
     */
    public double calculateFareWithSurge(double distanceKm, double surgeMultiplier) {
        double fare = (baseFare + (distanceKm * pricePerKm)) * surgeMultiplier;
        return Math.max(fare, minimumFare);
    }

    /**
     * Get estimated fare string
     */
    public String getEstimatedFareString(double distanceKm) {
        double fare = calculateFare(distanceKm);
        return String.format("Rs. %.0f", fare);
    }

    /**
     * Get formatted price string
     */
    public String getPriceString() {
        return String.format("Rs. %.0f/km", pricePerKm);
    }

    /**
     * Get base fare string
     */
    public String getBaseFareString() {
        return String.format("Rs. %.0f", baseFare);
    }

    /**
     * Check if vehicle type is economy
     */
    public boolean isEconomy() {
        return "economy".equals(vehicleType);
    }

    /**
     * Check if vehicle type is premium
     */
    public boolean isPremium() {
        return "premium".equals(vehicleType);
    }

    /**
     * Check if vehicle type is XL
     */
    public boolean isXl() {
        return "xl".equals(vehicleType);
    }

    /**
     * Check if vehicle type is bike
     */
    public boolean isBike() {
        return "bike".equals(vehicleType);
    }

    /**
     * Check if vehicle type is rickshaw
     */
    public boolean isRickshaw() {
        return "rickshaw".equals(vehicleType);
    }

    /**
     * Get capacity description
     */
    public String getCapacityDescription() {
        if (capacity == 1) {
            return "1 seat";
        } else {
            return capacity + " seats";
        }
    }

    /**
     * Get luggage description
     */
    public String getLuggageDescription() {
        if (luggageCapacity == 0) {
            return "No luggage";
        } else if (luggageCapacity == 1) {
            return "1 bag";
        } else {
            return luggageCapacity + " bags";
        }
    }

    /**
     * Get features as list of strings
     */
    public String[] getFeaturesList() {
        java.util.ArrayList<String> features = new java.util.ArrayList<>();

        features.add(getCapacityDescription());
        features.add(getLuggageDescription());

        if (hasAC) features.add("AC");
        if (hasWiFi) features.add("WiFi");
        if (hasChildSeat) features.add("Child Seat");
        if (isWheelchairAccessible) features.add("Wheelchair Accessible");

        return features.toArray(new String[0]);
    }

    /**
     * Get features as comma-separated string
     */
    public String getFeaturesString() {
        return android.text.TextUtils.join(" • ", getFeaturesList());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ServiceType that = (ServiceType) obj;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ServiceType{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", pricePerKm=" + pricePerKm +
                ", capacity=" + capacity +
                '}';
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Create Economy service type
     */
    public static ServiceType createEconomy() {
        return new ServiceType("economy", "Economy", "economy", 15.0, 4, R.drawable.ic_economy_car);
    }

    /**
     * Create Premium service type
     */
    public static ServiceType createPremium() {
        return new ServiceType("premium", "Premium", "premium", 25.0, 4, R.drawable.ic_premium_car);
    }

    /**
     * Create XL service type
     */
    public static ServiceType createXl() {
        return new ServiceType("xl", "XL", "xl", 35.0, 6, R.drawable.ic_suv);
    }

    /**
     * Create Bike service type
     */
    public static ServiceType createBike() {
        ServiceType bike = new ServiceType("bike", "Bike", "bike", 10.0, 1, R.drawable.ic_bike);
        bike.setBaseFare(30.0);
        bike.setMinimumFare(50.0);
        return bike;
    }

    /**
     * Create Rickshaw service type
     */
    public static ServiceType createRickshaw() {
        ServiceType rickshaw = new ServiceType("rickshaw", "Rickshaw", "rickshaw", 12.0, 3, R.drawable.ic_rickshaw);
        rickshaw.setBaseFare(40.0);
        rickshaw.setMinimumFare(60.0);
        return rickshaw;
    }

    /**
     * Get default list of service types
     */
    public static java.util.List<ServiceType> getDefaultList() {
        java.util.ArrayList<ServiceType> list = new java.util.ArrayList<>();
        list.add(createEconomy());
        list.add(createPremium());
        list.add(createXl());
        // list.add(createBike()); // Uncomment for bike option
        // list.add(createRickshaw()); // Uncomment for rickshaw option
        return list;
    }
}