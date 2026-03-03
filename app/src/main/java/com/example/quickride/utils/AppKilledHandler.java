package com.example.quickride.utils;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.firebase.geofire.GeoFire;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * Service that handles app termination events
 * Ensures driver is properly disconnected from the database
 * when app is killed or removed from recent apps
 */
public class AppKilledHandler extends Service {

    private static final String TAG = "AppKilledHandler";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Service is started but we don't want it to restart automatically
        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        // App was removed from recent apps (swiped away)
        handleAppTermination("task_removed");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Service is being destroyed
        // Note: This may not always be called when app is killed
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        // App is low on memory, might be killed
        // We'll handle this proactively
        handleAppTermination("low_memory");
    }

    /**
     * Handle app termination by cleaning up driver state
     * @param reason Reason for termination
     */
    private void handleAppTermination(String reason) {
        // Check if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 1. Remove from GeoFire locations
        removeDriverFromGeoFire(userId);

        // 2. Update driver status in database
        updateDriverStatus(userId, reason);
    }

    /**
     * Remove driver from GeoFire locations
     */
    private void removeDriverFromGeoFire(String userId) {
        try {
            // Remove from driversWorking
            DatabaseReference workingRef = FirebaseDatabase.getInstance()
                    .getReference("driversWorking");
            GeoFire geoFireWorking = new GeoFire(workingRef);
            geoFireWorking.removeLocation(userId);

            // Also remove from driversAvailable if present
            DatabaseReference availableRef = FirebaseDatabase.getInstance()
                    .getReference("driversAvailable");
            GeoFire geoFireAvailable = new GeoFire(availableRef);
            geoFireAvailable.removeLocation(userId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Update driver status in database
     */
    private void updateDriverStatus(String userId, String reason) {
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("isOnline", false);
            updates.put("lastSeen", System.currentTimeMillis());
            updates.put("disconnectReason", reason);

            FirebaseDatabase.getInstance()
                    .getReference()
                    .child("Users")
                    .child("Drivers")
                    .child(userId)
                    .updateChildren(updates);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Static method to start this service
     */
    public static void start(Intent intent) {
        // This method is kept for backward compatibility
        // The service is started automatically by the system
    }
}