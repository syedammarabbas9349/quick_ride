package com.example.quickride.utils;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.text.TextUtils;


import com.example.quickride.R;
import com.example.quickride.models.ServiceType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * Utility class with helper methods for the entire app
 */
public class Utils {

    // ==================== PRICE CALCULATION ====================

    /**
     * Calculate ride cost estimate
     * Base formula: baseFare + (distance * perKmRate) + (duration * perMinuteRate)
     */
    public static double calculateFare(double distanceKm, double pricePerKm, double baseFare) {
        double fare = baseFare + (distanceKm * pricePerKm);
        return Math.max(fare, getMinimumFare());
    }

    /**
     * Calculate ride cost with time component
     */
    public static double calculateFareWithTime(double distanceKm, double pricePerKm,
                                               double baseFare, long durationMinutes,
                                               double perMinuteRate) {
        double distanceFare = baseFare + (distanceKm * pricePerKm);
        double timeFare = durationMinutes * perMinuteRate;
        return Math.max(distanceFare + timeFare, getMinimumFare());
    }

    /**
     * Calculate ride cost with surge pricing
     */
    public static double calculateFareWithSurge(double distanceKm, double pricePerKm,
                                                double baseFare, double surgeMultiplier) {
        double fare = (baseFare + (distanceKm * pricePerKm)) * surgeMultiplier;
        return Math.max(fare, getMinimumFare());
    }

    /**
     * Get minimum fare
     */
    public static double getMinimumFare() {
        return 100.0; // Rs. 100 minimum
    }

    /**
     * Format price with currency symbol
     */
    public static String formatPrice(double price) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return "Rs. " + df.format(price);
    }

    /**
     * Format price without currency symbol
     */
    public static String formatPricePlain(double price) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(price);
    }

    // ==================== DISTANCE CALCULATION ====================

    /**
     * Calculate distance between two locations in kilometers
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return results[0] / 1000.0; // Convert to km
    }

    /**
     * Calculate distance between two locations in meters
     */
    public static float calculateDistanceInMeters(double lat1, double lng1, double lat2, double lng2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return results[0];
    }

    /**
     * Format distance for display
     */
    public static String formatDistance(double distanceKm) {
        if (distanceKm < 1) {
            int meters = (int) (distanceKm * 1000);
            return meters + " m";
        } else {
            DecimalFormat df = new DecimalFormat("#.#");
            return df.format(distanceKm) + " km";
        }
    }

    // ==================== TIME FORMATTING ====================

    /**
     * Format duration in seconds to readable string
     */
    public static String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "%d min", minutes);
        } else {
            return String.format(Locale.getDefault(), "%d sec", secs);
        }
    }

    /**
     * Format duration in minutes
     */
    public static String formatDurationMinutes(long minutes) {
        if (minutes < 60) {
            return minutes + " min";
        } else {
            long hours = minutes / 60;
            long mins = minutes % 60;
            return hours + "h " + mins + "m";
        }
    }

    /**
     * Get time ago string
     */
    public static String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) { // less than 1 minute
            return "Just now";
        } else if (diff < 3600000) { // less than 1 hour
            long minutes = diff / 60000;
            return minutes + " min ago";
        } else if (diff < 86400000) { // less than 1 day
            long hours = diff / 3600000;
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (diff < 604800000) { // less than 1 week
            long days = diff / 86400000;
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    /**
     * Format date for display
     */
    public static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    /**
     * Format date only (no time)
     */
    public static String formatDateOnly(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    /**
     * Format time only
     */
    public static String formatTimeOnly(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    // ==================== NUMBER FORMATTING ====================

    /**
     * Round a double value to specific decimal places
     */
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Round a float value to specific decimal places
     */
    /**
     * Format number with commas
     */
    public static String formatNumber(double number) {
        NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
        return format.format(number);
    }

    // ==================== VALIDATION ====================

    /**
     * Validate Pakistani mobile number
     */
    public static boolean isValidPakistaniMobile(String number) {
        if (TextUtils.isEmpty(number)) return false;
        String cleaned = number.replaceAll("[\\s-]", "");
        return cleaned.matches("^(03|3)\\d{9}$");
    }

    /**
     * Validate email address
     */
    public static boolean isValidEmail(String email) {
        if (TextUtils.isEmpty(email)) return false;
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Format mobile number for display
     */
    public static String formatMobileNumber(String number) {
        if (TextUtils.isEmpty(number)) return number;

        String cleaned = number.replaceAll("[\\s-]", "");
        if (cleaned.length() == 10) {
            return "0" + cleaned.substring(0, 3) + "-" + cleaned.substring(3, 6) + "-" + cleaned.substring(6);
        } else if (cleaned.length() == 11) {
            return cleaned.substring(0, 4) + "-" + cleaned.substring(4, 7) + "-" + cleaned.substring(7);
        }
        return number;
    }

    // ==================== SERVICE TYPES ====================

    /**
     * Get list of available service types
     */
    public static List<ServiceType> getServiceTypes(Context context) {
        List<ServiceType> types = new ArrayList<>();

        types.add(new ServiceType(
                "economy",
                context.getString(R.string.economy),
                "economy",
                15.0,
                4,
                R.drawable.ic_economy_car
        ));

        types.add(new ServiceType(
                "premium",
                context.getString(R.string.premium),
                "premium",
                25.0,
                4,
                R.drawable.ic_premium_car
        ));

        types.add(new ServiceType(
                "xl",
                context.getString(R.string.xl),
                "xl",
                35.0,
                6,
                R.drawable.ic_suv
        ));

        // Optional types
        // types.add(createBikeType(context));
        // types.add(createRickshawType(context));

        return types;
    }

    /**
     * Get service type by ID
     */
    public static ServiceType getServiceTypeById(Context context, String id) {
        List<ServiceType> types = getServiceTypes(context);
        for (ServiceType type : types) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return null;
    }

    // ==================== ETA CALCULATION ====================

    /**
     * Calculate estimated time of arrival in minutes
     * @param distanceKm distance in kilometers
     * @param averageSpeedKmH average speed in km/h
     */
    public static int calculateEta(double distanceKm, double averageSpeedKmH) {
        if (averageSpeedKmH <= 0) averageSpeedKmH = 30; // Default 30 km/h
        double hours = distanceKm / averageSpeedKmH;
        return (int) Math.ceil(hours * 60);
    }

    /**
     * Calculate ETA based on traffic conditions
     */
    public static int calculateEtaWithTraffic(double distanceKm, String trafficLevel) {
        double speed;
        switch (trafficLevel) {
            case "heavy":
                speed = 15;
                break;
            case "moderate":
                speed = 25;
                break;
            case "light":
                speed = 35;
                break;
            default:
                speed = 30;
                break;
        }
        return calculateEta(distanceKm, speed);
    }

    // ==================== SAFETY ====================

    /**
     * Mask sensitive information
     */
    public static String maskString(String input, int visibleChars) {
        if (input == null || input.length() <= visibleChars) {
            return input;
        }

        int maskLength = input.length() - visibleChars;
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < maskLength; i++) {
            masked.append("•");
        }
        masked.append(input.substring(maskLength));
        return masked.toString();
    }

    /**
     * Mask mobile number (show only last 4 digits)
     */
    public static String maskMobileNumber(String number) {
        if (TextUtils.isEmpty(number) || number.length() < 7) return number;
        String cleaned = number.replaceAll("[\\s-]", "");
        int length = cleaned.length();
        return "••••" + cleaned.substring(length - 4);
    }

    // ==================== SHARE TEXT ====================

    /**
     * Generate share text for ride
     */
    public static String getRideShareText(String pickup, String destination,
                                          double distance, double fare) {
        return String.format(Locale.getDefault(),
                "I'm taking a ride with QuickRide!\n\n" +
                        "From: %s\n" +
                        "To: %s\n" +
                        "Distance: %.1f km\n" +
                        "Fare: Rs. %.0f\n\n" +
                        "Download QuickRide: https://play.google.com/store/apps/details?id=com.example.quickride",
                pickup, destination, distance, fare);
    }

    /**
     * Generate share text for referral
     */
    public static String getReferralShareText(String referralCode) {
        return String.format(Locale.getDefault(),
                "Join me on QuickRide! Use my referral code %s to get Rs. 100 off your first ride.\n\n" +
                        "Download QuickRide: https://play.google.com/store/apps/details?id=com.example.quickride",
                referralCode);
    }

    // ==================== DP / SP CONVERSION ====================

    /**
     * Convert dp to pixels
     */
    public static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Convert pixels to dp
     */
    public static int pxToDp(Context context, int px) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(px / density);
    }

    // ==================== LEGACY METHODS (for backward compatibility) ====================

    /**
     * @deprecated Use round(double, int) instead
     */
    @Deprecated
    public BigDecimal round(float amount, int decimalPlace) {
        return BigDecimal.valueOf(amount).setScale(decimalPlace, RoundingMode.HALF_UP);
    }

    /**
     * @deprecated Use getServiceTypes(Context) instead
     */
    @Deprecated
    public static ArrayList<ServiceType> getTypeList(Activity activity) {
        return new ArrayList<>(getServiceTypes(activity));
    }

    /**
     * @deprecated Use calculateFare(double, double, double) instead
     */
    @Deprecated
    public static int rideCostEstimate(double distance, double duration) {
        double price = 36 + distance * 26 + duration * 0.001;
        return (int) price;
    }

    /**
     * @deprecated Use getServiceTypeById(Context, String) instead
     */
    @Deprecated
    public static ServiceType getTypeById(Activity activity, String id) {
        return getServiceTypeById(activity, id);
    }
}