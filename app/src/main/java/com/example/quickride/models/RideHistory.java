package com.example.quickride.models;

public class RideHistory {
    private String rideId;
    private String customerId;
    private String driverId;
    private String customerName;
    private String driverName;
    private String pickupAddress;
    private String destinationAddress;
    private double pickupLat;
    private double pickupLng;
    private double destLat;
    private double destLng;
    private double distance;
    private String duration;
    private double fare;
    private String paymentMethod;
    private long timestamp;
    private double rating;
    private String customerImageUrl;
    private String driverImageUrl;
    private String status;
    private String carInfo;  // Add this field
    private String driverPhone;
    private String customerPhone;
    private double driverRating;

    // Empty constructor
    public RideHistory() {}

    // Getters and setters for all fields
    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

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

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public double getFare() { return fare; }
    public void setFare(double fare) { this.fare = fare; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public String getCustomerImageUrl() { return customerImageUrl; }
    public void setCustomerImageUrl(String customerImageUrl) { this.customerImageUrl = customerImageUrl; }

    public String getDriverImageUrl() { return driverImageUrl; }
    public void setDriverImageUrl(String driverImageUrl) { this.driverImageUrl = driverImageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Add these new getters and setters
    public String getCarInfo() { return carInfo; }
    public void setCarInfo(String carInfo) { this.carInfo = carInfo; }

    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public double getDriverRating() { return driverRating; }
    public void setDriverRating(double driverRating) { this.driverRating = driverRating; }
}