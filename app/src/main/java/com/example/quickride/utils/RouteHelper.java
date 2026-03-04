package com.example.quickride.utils;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Helper class for Google Directions API
 * Handles route calculation between two points
 */
public class RouteHelper {

    private static final String TAG = "RouteHelper";
    private static final String DIRECTIONS_API_URL = "https://maps.googleapis.com/maps/api/directions/json?";

    private Context context;
    private String apiKey;
    private RouteCallback callback;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public interface RouteCallback {
        void onRouteSuccess(ArrayList<LatLng> path, double distance, int duration);
        void onRouteFailure(String error);
    }

    public RouteHelper(Context context, String apiKey) {
        this.context = context;
        this.apiKey = apiKey;
    }

    public void setCallback(RouteCallback callback) {
        this.callback = callback;
    }/**
     * Get route with callback
     */
    public void getRoute(LatLng origin, LatLng destination, RouteCallback callback) {
        this.callback = callback;
        getRoute(origin, destination);
    }


    /**
     * Get route between origin and destination
     */
    public void getRoute(LatLng origin, LatLng destination) {
        String url = getDirectionsUrl(origin, destination);
        executorService.execute(new DownloadTask(url));
    }

    /**
     * Get route with waypoints
     */
    public void getRouteWithWaypoints(LatLng origin, LatLng destination, List<LatLng> waypoints) {
        StringBuilder waypointsStr = new StringBuilder();
        for (LatLng point : waypoints) {
            if (waypointsStr.length() > 0) {
                waypointsStr.append("|");
            }
            waypointsStr.append(point.latitude).append(",").append(point.longitude);
        }

        String url = DIRECTIONS_API_URL +
                "origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&waypoints=" + waypointsStr.toString() +
                "&mode=driving" +
                "&key=" + apiKey;

        executorService.execute(new DownloadTask(url));
    }

    /**
     * Get directions URL
     */
    private String getDirectionsUrl(LatLng origin, LatLng destination) {
        return DIRECTIONS_API_URL +
                "origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&mode=driving" +
                "&alternatives=true" +
                "&key=" + apiKey;
    }

    /**
     * Download task for fetching route data
     */
    private class DownloadTask implements Runnable {
        private String url;

        DownloadTask(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            String data = "";
            try {
                data = downloadUrl(url);
            } catch (IOException e) {
                Log.e(TAG, "Download error: " + e.getMessage());
                if (callback != null) {
                    callback.onRouteFailure("Network error: " + e.getMessage());
                }
                return;
            }

            parseJson(data);
        }
    }

    /**
     * Download data from URL
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;

        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();

            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            br.close();

        } catch (Exception e) {
            Log.e(TAG, "Download exception: " + e.getMessage());
            throw new IOException("Error downloading data", e);
        } finally {
            if (iStream != null) {
                iStream.close();
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return data;
    }

    /**
     * Parse JSON response from Directions API
     */
    private void parseJson(String jsonData) {
        ArrayList<LatLng> path = new ArrayList<>();
        double distance = 0;
        int duration = 0;

        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            String status = jsonObject.getString("status");

            if (!"OK".equals(status)) {
                String errorMessage = jsonObject.optString("error_message", "Unknown error");
                Log.e(TAG, "Directions API error: " + status + " - " + errorMessage);

                if (callback != null) {
                    callback.onRouteFailure("API error: " + status + " - " + errorMessage);
                }
                return;
            }

            JSONArray routes = jsonObject.getJSONArray("routes");

            if (routes.length() > 0) {
                JSONObject route = routes.getJSONObject(0);
                JSONArray legs = route.getJSONArray("legs");

                // Calculate total distance and duration
                for (int i = 0; i < legs.length(); i++) {
                    JSONObject leg = legs.getJSONObject(i);
                    distance += leg.getJSONObject("distance").getDouble("value");
                    duration += leg.getJSONObject("duration").getInt("value");
                }

                // Get overview polyline
                JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                String encodedPoints = overviewPolyline.getString("points");
                path = (ArrayList<LatLng>) PolyUtil.decode(encodedPoints);

                // Convert distance to km
                distance = distance / 1000.0;

                if (callback != null) {
                    callback.onRouteSuccess(path, distance, duration);
                }
            } else {
                if (callback != null) {
                    callback.onRouteFailure("No routes found");
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error: " + e.getMessage());
            if (callback != null) {
                callback.onRouteFailure("Error parsing route data");
            }
        }
    }

    /**
     * Get polyline options for drawing route
     */
    public static PolylineOptions createPolylineOptions(List<LatLng> points, int color) {
        return new PolylineOptions()
                .addAll(points)
                .width(12)
                .color(color)
                .geodesic(true);
    }

    /**
     * Calculate distance between two points (static utility)
     */
    public static float calculateDistance(LatLng start, LatLng end) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(
                start.latitude, start.longitude,
                end.latitude, end.longitude,
                results);
        return results[0];
    }

    /**
     * Format duration from seconds to readable string
     */
    public static String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d hr %d min", hours, minutes);
        } else {
            return String.format(Locale.getDefault(), "%d min", minutes);
        }
    }

    /**
     * Format distance from km to readable string
     */
    public static String formatDistance(double km) {
        if (km < 1) {
            int meters = (int) (km * 1000);
            return String.format(Locale.getDefault(), "%d m", meters);
        } else {
            return String.format(Locale.getDefault(), "%.1f km", km);
        }
    }

    /**
     * Shutdown executor service
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}