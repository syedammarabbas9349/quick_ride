package com.example.quickride.utils;

import android.location.Location;
import android.util.Log;

import com.example.quickride.models.RideRequest;
import com.example.quickride.models.SharedPassenger;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages shared ride logic: matching, fare splitting, route optimization
 */
public class SharedRideManager {

    private static final String TAG = "SharedRideManager";
    private static final double MAX_DETOUR_RATIO = 0.3; // 30% max detour
    private static final int PICKUP_WAIT_TIME = 120; // 2 minutes in seconds
    private static final double AVERAGE_SPEED = 8.33; // 30 km/h in m/s

    /**
     * Calculate fare for sharing (discounted)
     */
    public static double calculateSharingFare(double originalFare, double sharingDiscount) {
        return originalFare * (1 - sharingDiscount);
    }

    /**
     * Check if a new passenger can be added to an existing shared ride
     */
    public static boolean canAddPassenger(RideRequest currentRide,
                                          SharedPassenger newPassenger,
                                          LatLng currentDriverLocation) {

        if (!currentRide.isSharingEnabled() ||
                currentRide.getCurrentPassengers() >= currentRide.getMaxPassengers()) {
            Log.d(TAG, "Cannot add: sharing disabled or full");
            return false;
        }

        // Calculate current route distance
        List<LatLng> currentRoute = buildCurrentRoute(currentRide, currentDriverLocation);
        double currentDistance = calculateRouteDistance(currentRoute);

        // Calculate new route with passenger
        List<LatLng> newRoute = buildRouteWithPassenger(currentRide, newPassenger, currentDriverLocation);
        double newDistance = calculateRouteDistance(newRoute);

        // Check if detour is acceptable
        double detourRatio = (newDistance - currentDistance) / currentDistance;
        boolean acceptable = detourRatio <= MAX_DETOUR_RATIO;

        Log.d(TAG, "Detour ratio: " + detourRatio + ", Acceptable: " + acceptable);
        return acceptable;
    }

    /**
     * Build current route (driver location → pending pickups → dropoffs → final destination)
     */
    private static List<LatLng> buildCurrentRoute(RideRequest ride, LatLng currentLocation) {
        List<LatLng> route = new ArrayList<>();
        route.add(currentLocation);

        // Add all pending pickups
        if (ride.getPassengers() != null) {
            for (SharedPassenger passenger : ride.getPassengers()) {
                if ("pending".equals(passenger.getStatus())) {
                    route.add(passenger.getPickupLatLng());
                }
            }

            // Add all onboard dropoffs
            for (SharedPassenger passenger : ride.getPassengers()) {
                if ("onboard".equals(passenger.getStatus())) {
                    route.add(passenger.getDropoffLatLng());
                }
            }
        }

        // Add final destination
        route.add(ride.getDestinationLocation().getLatLng());

        return route;
    }

    /**
     * Build route with new passenger
     */
    private static List<LatLng> buildRouteWithPassenger(RideRequest ride,
                                                        SharedPassenger newPassenger,
                                                        LatLng currentLocation) {
        List<LatLng> route = new ArrayList<>();
        route.add(currentLocation);

        // Add new passenger's pickup
        route.add(newPassenger.getPickupLatLng());

        // Add existing pending pickups
        if (ride.getPassengers() != null) {
            for (SharedPassenger passenger : ride.getPassengers()) {
                if ("pending".equals(passenger.getStatus())) {
                    route.add(passenger.getPickupLatLng());
                }
            }
        }

        // Add new passenger's dropoff
        route.add(newPassenger.getDropoffLatLng());

        // Add existing onboard dropoffs
        if (ride.getPassengers() != null) {
            for (SharedPassenger passenger : ride.getPassengers()) {
                if ("onboard".equals(passenger.getStatus())) {
                    route.add(passenger.getDropoffLatLng());
                }
            }
        }

        // Add final destination
        route.add(ride.getDestinationLocation().getLatLng());

        return route;
    }

    /**
     * Calculate total distance of a route in meters
     */
    public static double calculateRouteDistance(List<LatLng> route) {
        double totalDistance = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            totalDistance += distanceBetween(route.get(i), route.get(i + 1));
        }
        return totalDistance;
    }

    /**
     * Calculate distance between two LatLng points in meters
     */
    private static double distanceBetween(LatLng point1, LatLng point2) {
        float[] results = new float[1];
        Location.distanceBetween(
                point1.latitude, point1.longitude,
                point2.latitude, point2.longitude,
                results
        );
        return results[0];
    }

    /**
     * Split fare among all passengers based on distance traveled
     */
    public static Map<String, Double> splitFare(RideRequest ride, List<LatLng> actualRoute) {
        Map<String, Double> passengerFares = new HashMap<>();

        if (ride.getPassengers() == null || ride.getPassengers().isEmpty()) {
            return passengerFares;
        }

        // Calculate segment distances
        List<Double> segmentDistances = new ArrayList<>();
        for (int i = 0; i < actualRoute.size() - 1; i++) {
            segmentDistances.add(distanceBetween(actualRoute.get(i), actualRoute.get(i + 1)));
        }

        double totalDistance = 0;
        for (double d : segmentDistances) totalDistance += d;
        double totalFare = ride.getFare() * ride.getPassengers().size();

        // For each passenger, calculate their share
        for (SharedPassenger passenger : ride.getPassengers()) {
            double passengerDistance = 0;

            int startIdx = findRouteIndex(actualRoute, passenger.getPickupLatLng());
            int endIdx = findRouteIndex(actualRoute, passenger.getDropoffLatLng());

            for (int i = startIdx; i < endIdx && i < segmentDistances.size(); i++) {
                passengerDistance += segmentDistances.get(i);
            }

            double fareShare = (passengerDistance / totalDistance) * totalFare;
            passengerFares.put(passenger.getUserId(), fareShare);
            passenger.setFareShare(fareShare);
        }

        return passengerFares;
    }

    /**
     * Find index of a point in route (approximate)
     */
    private static int findRouteIndex(List<LatLng> route, LatLng point) {
        int bestIndex = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < route.size(); i++) {
            double distance = distanceBetween(route.get(i), point);
            if (distance < minDistance) {
                minDistance = distance;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    /**
     * Calculate ETA for a shared ride
     */
    public static int calculateETA(List<LatLng> route) {
        double totalDistance = calculateRouteDistance(route);
        int totalSeconds = (int) (totalDistance / AVERAGE_SPEED);
        totalSeconds += (route.size() - 1) * PICKUP_WAIT_TIME;
        return totalSeconds;
    }

    /**
     * Update shared ride in Firebase when new passenger joins
     */
    public static void updateSharedRide(RideRequest ride, SharedPassenger newPassenger) {
        if (ride.getShareRideId() == null) return;

        DatabaseReference sharedRideRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("shared_rides")
                .child(ride.getShareRideId());

        // Add passenger to list
        if (ride.getPassengers() == null) {
            ride.setPassengers(new ArrayList<>());
        }
        ride.getPassengers().add(newPassenger);
        ride.setCurrentPassengers(ride.getCurrentPassengers() + 1);

        Map<String, Object> updates = new HashMap<>();
        updates.put("passengers", ride.getPassengers());
        updates.put("currentPassengers", ride.getCurrentPassengers());

        sharedRideRef.updateChildren(updates);
        Log.d(TAG, "Shared ride updated. Now has " + ride.getCurrentPassengers() + " passengers");
    }

    /**
     * Get sharing discount text
     */
    public static String getSharingDiscountText(double sharingDiscount) {
        return "Save " + (int) (sharingDiscount * 100) + "%";
    }

    /**
     * Format sharing status for display
     */
    public static String getSharingStatusText(RideRequest ride) {
        if (!ride.isSharingEnabled()) return "Private Ride";
        if (ride.getCurrentPassengers() >= ride.getMaxPassengers()) return "Full";
        return "Sharing • " + ride.getCurrentPassengers() + "/" + ride.getMaxPassengers() + " passengers";
    }
}