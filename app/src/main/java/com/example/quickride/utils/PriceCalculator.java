package com.example.quickride.utils;

/**
 * Utility class for calculating ride prices
 * Supports different vehicle types with base fare, per km rate, and minimum fare
 */
public class PriceCalculator {

    // Default rates (can be overridden by vehicle type)
    private static final double DEFAULT_BASE_FARE = 50.0;
    private static final double DEFAULT_PER_KM_RATE = 15.0;
    private static final double DEFAULT_PER_MINUTE_RATE = 2.0;
    private static final double DEFAULT_MINIMUM_FARE = 100.0;

    // Surge pricing multiplier (1.0 = normal, 1.5 = 50% surge, 2.0 = double)
    private static double surgeMultiplier = 1.0;

    /**
     * Calculate fare based on distance
     *
     * @param distanceKm Distance in kilometers
     * @return Calculated fare
     */
    public static double calculateFare(double distanceKm) {
        return calculateFare(distanceKm, DEFAULT_BASE_FARE, DEFAULT_PER_KM_RATE, DEFAULT_MINIMUM_FARE);
    }

    /**
     * Calculate fare based on distance with custom rates
     * THIS IS THE METHOD YOU'RE CALLING
     *
     * @param distanceKm Distance in kilometers
     * @param baseFare Base fare amount
     * @param perKmRate Rate per kilometer
     * @param minimumFare Minimum fare amount
     * @return Calculated fare
     */
    public static double calculateFare(double distanceKm, double baseFare,
                                       double perKmRate, double minimumFare) {
        double fare = baseFare + (distanceKm * perKmRate);
        return Math.max(fare, minimumFare) * surgeMultiplier;
    }

    /**
     * Calculate fare based on distance, price per km, and base fare
     * Uses default minimum fare
     *
     * @param distanceKm Distance in kilometers
     * @param pricePerKm Rate per kilometer
     * @param baseFare Base fare amount
     * @return Calculated fare
     */
    public static double calculateFare(double distanceKm, double pricePerKm, double baseFare) {
        double fare = baseFare + (distanceKm * pricePerKm);
        return Math.max(fare, DEFAULT_MINIMUM_FARE) * surgeMultiplier;
    }

    /**
     * Calculate fare with time component (for traffic/waiting)
     *
     * @param distanceKm Distance in kilometers
     * @param durationMinutes Duration in minutes
     * @return Calculated fare with time component
     */
    public static double calculateFareWithTime(double distanceKm, long durationMinutes) {
        return calculateFareWithTime(distanceKm, durationMinutes,
                DEFAULT_BASE_FARE, DEFAULT_PER_KM_RATE, DEFAULT_PER_MINUTE_RATE, DEFAULT_MINIMUM_FARE);
    }

    /**
     * Calculate fare with time component using custom rates
     *
     * @param distanceKm Distance in kilometers
     * @param durationMinutes Duration in minutes
     * @param baseFare Base fare amount
     * @param perKmRate Rate per kilometer
     * @param perMinuteRate Rate per minute
     * @param minimumFare Minimum fare amount
     * @return Calculated fare
     */
    public static double calculateFareWithTime(double distanceKm, long durationMinutes,
                                               double baseFare, double perKmRate,
                                               double perMinuteRate, double minimumFare) {
        double distanceFare = baseFare + (distanceKm * perKmRate);
        double timeFare = durationMinutes * perMinuteRate;
        return Math.max(distanceFare + timeFare, minimumFare) * surgeMultiplier;
    }

    /**
     * Calculate fare with surge pricing
     *
     * @param distanceKm Distance in kilometers
     * @param surgeMultiplier Surge multiplier (1.0 = normal)
     * @return Calculated fare with surge
     */
    public static double calculateFareWithSurge(double distanceKm, double surgeMultiplier) {
        double fare = DEFAULT_BASE_FARE + (distanceKm * DEFAULT_PER_KM_RATE);
        return Math.max(fare, DEFAULT_MINIMUM_FARE) * surgeMultiplier;
    }

    /**
     * Calculate fare for specific vehicle type
     *
     * @param distanceKm Distance in kilometers
     * @param vehicleType Vehicle type (economy, premium, xl)
     * @return Calculated fare for vehicle type
     */
    public static double calculateFareForVehicle(double distanceKm, String vehicleType) {
        double baseFare = DEFAULT_BASE_FARE;
        double perKmRate = DEFAULT_PER_KM_RATE;
        double minimumFare = DEFAULT_MINIMUM_FARE;

        switch (vehicleType.toLowerCase()) {
            case "economy":
                baseFare = 50.0;
                perKmRate = 15.0;
                minimumFare = 100.0;
                break;
            case "premium":
                baseFare = 80.0;
                perKmRate = 25.0;
                minimumFare = 150.0;
                break;
            case "xl":
                baseFare = 100.0;
                perKmRate = 35.0;
                minimumFare = 200.0;
                break;
            case "bike":
                baseFare = 30.0;
                perKmRate = 10.0;
                minimumFare = 50.0;
                break;
            case "rickshaw":
                baseFare = 40.0;
                perKmRate = 12.0;
                minimumFare = 60.0;
                break;
        }

        return calculateFare(distanceKm, baseFare, perKmRate, minimumFare);
    }

    /**
     * Set surge pricing multiplier
     *
     * @param multiplier Surge multiplier (1.0 = normal)
     */
    public static void setSurgeMultiplier(double multiplier) {
        surgeMultiplier = Math.max(1.0, multiplier);
    }

    /**
     * Get current surge multiplier
     *
     * @return Current surge multiplier
     */
    public static double getSurgeMultiplier() {
        return surgeMultiplier;
    }

    /**
     * Calculate driver earnings after commission
     *
     * @param fare Total fare
     * @param commissionRate Commission rate (0.20 = 20%)
     * @return Driver earnings
     */
    public static double calculateDriverEarnings(double fare, double commissionRate) {
        return fare * (1 - commissionRate);
    }

    /**
     * Format price with currency symbol
     *
     * @param price Price to format
     * @return Formatted price string
     */
    public static String formatPrice(double price) {
        return String.format("Rs. %.0f", price);
    }

    /**
     * Format price with decimal places
     *
     * @param price Price to format
     * @return Formatted price string with decimals
     */
    public static String formatPriceDetailed(double price) {
        return String.format("Rs. %.2f", price);
    }
}