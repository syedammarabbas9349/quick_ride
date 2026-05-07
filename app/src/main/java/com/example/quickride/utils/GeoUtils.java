package com.example.quickride.utils;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

/**
 * Utility class for geographic calculations
 */
public class GeoUtils {

    /**
     * Calculate distance between two LatLng points in meters
     */
    public static float calculateDistance(LatLng point1, LatLng point2) {
        float[] results = new float[1];
        Location.distanceBetween(
                point1.latitude, point1.longitude,
                point2.latitude, point2.longitude,
                results
        );
        return results[0];
    }

    /**
     * Calculate distance between two LatLng points in kilometers
     */
    public static double calculateDistanceKm(LatLng point1, LatLng point2) {
        return calculateDistance(point1, point2) / 1000.0;
    }

    /**
     * Check if a point is within radius of another point
     */
    public static boolean isWithinRadius(LatLng center, LatLng point, double radiusMeters) {
        return calculateDistance(center, point) <= radiusMeters;
    }

    /**
     * Calculate bearing between two points
     */
    public static float calculateBearing(LatLng from, LatLng to) {
        double lat1 = Math.toRadians(from.latitude);
        double lon1 = Math.toRadians(from.longitude);
        double lat2 = Math.toRadians(to.latitude);
        double lon2 = Math.toRadians(to.longitude);

        double longitude = lon2 - lon1;
        double y = Math.sin(longitude) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(longitude);

        return (float) (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    /**
     * Get midpoint between two points
     */
    public static LatLng getMidpoint(LatLng point1, LatLng point2) {
        double lat1 = Math.toRadians(point1.latitude);
        double lon1 = Math.toRadians(point1.longitude);
        double lat2 = Math.toRadians(point2.latitude);
        double lon2 = Math.toRadians(point2.longitude);

        double bx = Math.cos(lat2) * Math.cos(lon2 - lon1);
        double by = Math.cos(lat2) * Math.sin(lon2 - lon1);
        double lat3 = Math.atan2(Math.sin(lat1) + Math.sin(lat2),
                Math.sqrt((Math.cos(lat1) + bx) * (Math.cos(lat1) + bx) + by * by));
        double lon3 = lon1 + Math.atan2(by, Math.cos(lat1) + bx);

        return new LatLng(Math.toDegrees(lat3), Math.toDegrees(lon3));
    }
}