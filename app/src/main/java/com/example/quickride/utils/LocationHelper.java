package com.example.quickride.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.RequiresPermission;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;

/**
 * Helper class for location permissions and updates
 */
public class LocationHelper {

    private Context context;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationListener listener;
    private LatLng currentLocation;

    public interface LocationListener {
        void onLocationChanged(LatLng latLng);
        void onLocationError(String error);
    }

    public LocationHelper(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    /**
     * Check if location permission is granted
     */
    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request location updates
     */
    public void startLocationUpdates(LocationListener listener) {
        this.listener = listener;

        if (!hasLocationPermission()) {
            if (listener != null) {
                listener.onLocationError("Location permission not granted");
            }
            return;
        }


        LocationRequest locationRequest =
                new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                        .setMinUpdateIntervalMillis(2000)
                        .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    if (listener != null) {
                        listener.onLocationChanged(currentLocation);
                    }
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            if (listener != null) {
                listener.onLocationError("Security exception: " + e.getMessage());
            }
        }
    }

    /**
     * Stop location updates
     */
    public void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    /**
     * Get last known location
     */
    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    public void getLastLocation(LocationListener listener) {
        if (!hasLocationPermission()) {
            listener.onLocationError("Location permission not granted");
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                listener.onLocationChanged(currentLocation);
            } else {
                listener.onLocationError("Last location not available");
            }
        });
    }

    /**
     * Get current location (last known)
     */
    public void setCurrentLocation(LatLng location) {
        this.currentLocation = location;
    }
    public boolean hasCurrentLocation() {
        return currentLocation != null;
    }
    public LatLng getCurrentLocation() {
        return currentLocation;
    }

    /**
     * Calculate distance between two LatLng points
     */
    public static float distanceBetween(LatLng point1, LatLng point2) {
        float[] results = new float[1];
        Location.distanceBetween(
                point1.latitude, point1.longitude,
                point2.latitude, point2.longitude,
                results
        );
        return results[0];
    }

    /**
     * Calculate distance in kilometers
     */
    public static double distanceInKm(LatLng point1, LatLng point2) {
        return distanceBetween(point1, point2) / 1000.0;
    }
}