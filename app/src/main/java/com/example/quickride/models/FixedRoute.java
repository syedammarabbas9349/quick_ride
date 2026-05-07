package com.example.quickride.models;

import androidx.annotation.Keep;
import java.util.HashMap;
import java.util.Map;

@Keep
public class FixedRoute {
    private String routeId;
    private String driverId;
    private String driverName;
    private String driverImageUrl;
    private double rating;
    private String startPoint;
    private String destination;
    private String waypoints;
    private String departureTimeWindow;
    private String vehicleType;
    private int totalSeats;
    private int availableSeats;
    private double fixedFare;
    private double distance;
    private boolean active;
    private long createdAt;

    public FixedRoute() {}

    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getDriverImageUrl() { return driverImageUrl; }
    public void setDriverImageUrl(String driverImageUrl) { this.driverImageUrl = driverImageUrl; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public String getStartPoint() { return startPoint; }
    public void setStartPoint(String startPoint) { this.startPoint = startPoint; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getWaypoints() { return waypoints; }
    public void setWaypoints(String waypoints) { this.waypoints = waypoints; }

    public String getDepartureTimeWindow() { return departureTimeWindow; }
    public void setDepartureTimeWindow(String departureTimeWindow) { this.departureTimeWindow = departureTimeWindow; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public double getFixedFare() { return fixedFare; }
    public void setFixedFare(double fixedFare) { this.fixedFare = fixedFare; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("routeId", routeId);
        map.put("driverId", driverId);
        map.put("driverName", driverName);
        map.put("driverImageUrl", driverImageUrl);
        map.put("rating", rating);
        map.put("startPoint", startPoint);
        map.put("destination", destination);
        map.put("waypoints", waypoints);
        map.put("departureTimeWindow", departureTimeWindow);
        map.put("vehicleType", vehicleType);
        map.put("totalSeats", totalSeats);
        map.put("availableSeats", availableSeats);
        map.put("fixedFare", fixedFare);
        map.put("distance", distance);
        map.put("active", active);
        map.put("createdAt", createdAt);
        return map;
    }
}
