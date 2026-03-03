package com.example.quickride.models;

import android.content.Context;
import android.text.format.DateFormat;

import androidx.annotation.Keep;

import com.google.firebase.database.DataSnapshot;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import com.example.quickride.R;

/**
 * Payout model for driver earnings and withdrawals
 * Supports JazzCash, EasyPaisa, and Bank transfers
 */
@Keep
public class Payout {

    private String payoutId;
    private String driverId;
    private String period;           // e.g., "Week 12, 2024" or "March 2024"
    private double amount;
    private int rideCount;
    private double distance;          // Total distance in km
    private long duration;            // Total duration in minutes

    // Status fields
    private String status;            // "available", "pending", "completed", "cancelled"
    private String paymentMethod;      // "jazzcash", "easypaisa", "bank"
    private String accountDetails;     // Mobile number or bank account
    private String accountHolderName;

    // Timestamps
    private long earnedAt;             // When the earnings were accrued
    private long requestedAt;           // When withdrawal was requested
    private long processedAt;           // When payment was completed
    private long cancelledAt;           // If cancelled

    // Transaction details
    private String transactionId;
    private String transactionReference;
    private String notes;

    // For display
    private transient Context context;

    /**
     * Empty constructor required for Firebase
     */
    public Payout() {}

    /**
     * Constructor for new earnings
     */
    public Payout(String driverId, String period, double amount, int rideCount, double distance, long duration) {
        this.driverId = driverId;
        this.period = period;
        this.amount = amount;
        this.rideCount = rideCount;
        this.distance = distance;
        this.duration = duration;
        this.status = "available";
        this.earnedAt = System.currentTimeMillis();
    }

    /**
     * Constructor with context for date formatting
     */
    public Payout(Context context) {
        this.context = context;
    }

    /**
     * Parse DataSnapshot into this object
     */
    public void parseData(DataSnapshot dataSnapshot) {
        if (dataSnapshot == null) return;

        this.payoutId = dataSnapshot.getKey();

        if (dataSnapshot.child("driverId").getValue() != null) {
            this.driverId = dataSnapshot.child("driverId").getValue().toString();
        }
        if (dataSnapshot.child("period").getValue() != null) {
            this.period = dataSnapshot.child("period").getValue().toString();
        }
        if (dataSnapshot.child("amount").getValue() != null) {
            // Handle amount stored in cents/paisa (divide by 100)
            try {
                Object amountObj = dataSnapshot.child("amount").getValue();
                if (amountObj instanceof Long) {
                    this.amount = ((Long) amountObj) / 100.0;
                } else if (amountObj instanceof Double) {
                    this.amount = (Double) amountObj;
                } else {
                    this.amount = Double.parseDouble(amountObj.toString());
                }
            } catch (Exception e) {
                this.amount = 0.0;
            }
        }
        if (dataSnapshot.child("rideCount").getValue() != null) {
            this.rideCount = dataSnapshot.child("rideCount").getValue(Integer.class);
        }
        if (dataSnapshot.child("distance").getValue() != null) {
            this.distance = dataSnapshot.child("distance").getValue(Double.class);
        }
        if (dataSnapshot.child("duration").getValue() != null) {
            this.duration = dataSnapshot.child("duration").getValue(Long.class);
        }
        if (dataSnapshot.child("status").getValue() != null) {
            this.status = dataSnapshot.child("status").getValue().toString();
        }
        if (dataSnapshot.child("paymentMethod").getValue() != null) {
            this.paymentMethod = dataSnapshot.child("paymentMethod").getValue().toString();
        }
        if (dataSnapshot.child("accountDetails").getValue() != null) {
            this.accountDetails = dataSnapshot.child("accountDetails").getValue().toString();
        }
        if (dataSnapshot.child("accountHolderName").getValue() != null) {
            this.accountHolderName = dataSnapshot.child("accountHolderName").getValue().toString();
        }

        // Parse timestamps
        if (dataSnapshot.child("earnedAt").getValue() != null) {
            this.earnedAt = dataSnapshot.child("earnedAt").getValue(Long.class);
        }
        if (dataSnapshot.child("requestedAt").getValue() != null) {
            this.requestedAt = dataSnapshot.child("requestedAt").getValue(Long.class);
        }
        if (dataSnapshot.child("processedAt").getValue() != null) {
            this.processedAt = dataSnapshot.child("processedAt").getValue(Long.class);
        }
        if (dataSnapshot.child("cancelledAt").getValue() != null) {
            this.cancelledAt = dataSnapshot.child("cancelledAt").getValue(Long.class);
        }

        // Transaction details
        if (dataSnapshot.child("transactionId").getValue() != null) {
            this.transactionId = dataSnapshot.child("transactionId").getValue().toString();
        }
        if (dataSnapshot.child("transactionReference").getValue() != null) {
            this.transactionReference = dataSnapshot.child("transactionReference").getValue().toString();
        }
        if (dataSnapshot.child("notes").getValue() != null) {
            this.notes = dataSnapshot.child("notes").getValue().toString();
        }
    }

    // ==================== GETTERS & SETTERS ====================

    public String getPayoutId() { return payoutId; }
    public void setPayoutId(String payoutId) { this.payoutId = payoutId; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public int getRideCount() { return rideCount; }
    public void setRideCount(int rideCount) { this.rideCount = rideCount; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getAccountDetails() { return accountDetails; }
    public void setAccountDetails(String accountDetails) { this.accountDetails = accountDetails; }

    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }

    public long getEarnedAt() { return earnedAt; }
    public void setEarnedAt(long earnedAt) { this.earnedAt = earnedAt; }

    public long getRequestedAt() { return requestedAt; }
    public void setRequestedAt(long requestedAt) { this.requestedAt = requestedAt; }

    public long getProcessedAt() { return processedAt; }
    public void setProcessedAt(long processedAt) { this.processedAt = processedAt; }

    public long getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(long cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // ==================== UTILITY METHODS ====================

    /**
     * Get formatted amount with currency symbol
     */
    public String getFormattedAmount() {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return "Rs. " + df.format(amount);
    }

    /**
     * Get formatted amount without currency symbol
     */
    public String getAmountString() {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(amount);
    }

    /**
     * Get formatted date for earned date
     */
    public String getFormattedEarnedDate() {
        if (earnedAt == 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(new Date(earnedAt));
    }

    /**
     * Get formatted date for requested date
     */
    public String getFormattedRequestedDate() {
        if (requestedAt == 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(new Date(requestedAt));
    }

    /**
     * Get formatted date for processed date
     */
    public String getFormattedProcessedDate() {
        if (processedAt == 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(new Date(processedAt));
    }

    /**
     * Get formatted duration
     */
    public String getFormattedDuration() {
        long hours = duration / 60;
        long minutes = duration % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + " min";
        }
    }

    /**
     * Get status color resource ID
     */
    public int getStatusColor() {
        switch (status != null ? status.toLowerCase() : "") {
            case "available":
                return android.R.color.holo_green_dark;
            case "pending":
                return android.R.color.holo_orange_dark;
            case "completed":
            case "paid":
                return android.R.color.holo_blue_dark;
            case "cancelled":
                return android.R.color.holo_red_dark;
            default:
                return android.R.color.darker_gray;
        }
    }

    /**
     * Get status icon resource ID
     */
    public int getStatusIcon() {
        switch (status != null ? status.toLowerCase() : "") {
            case "available":
                return R.drawable.ic_available;
            case "pending":
                return R.drawable.ic_pending;
            case "completed":
            case "paid":
                return R.drawable.ic_check_circle;
            case "cancelled":
                return R.drawable.ic_cancel;
            default:
                return R.drawable.ic_info;
        }
    }

    /**
     * Check if payout can be withdrawn
     */
    public boolean isWithdrawable() {
        return "available".equals(status) && amount > 0;
    }

    /**
     * Check if JazzCash payment method
     */
    public boolean isJazzCash() {
        return "jazzcash".equals(paymentMethod);
    }

    /**
     * Check if EasyPaisa payment method
     */
    public boolean isEasyPaisa() {
        return "easypaisa".equals(paymentMethod);
    }

    /**
     * Check if Bank transfer
     */
    public boolean isBankTransfer() {
        return "bank".equals(paymentMethod);
    }

    /**
     * Get payment method display name
     */
    public String getPaymentMethodDisplay() {
        if (paymentMethod == null) return "Not specified";

        switch (paymentMethod.toLowerCase()) {
            case "jazzcash":
                return "JazzCash";
            case "easypaisa":
                return "EasyPaisa";
            case "bank":
                return "Bank Transfer";
            default:
                return paymentMethod;
        }
    }

    /**
     * Get masked account details for display
     */
    public String getMaskedAccountDetails() {
        if (accountDetails == null || accountDetails.isEmpty()) {
            return "";
        }

        if (isJazzCash() || isEasyPaisa()) {
            // Mask mobile number: 03XX-XXX-XXXX
            if (accountDetails.length() >= 10) {
                return accountDetails.substring(0, 4) + "-•••-" +
                        accountDetails.substring(accountDetails.length() - 4);
            }
        }
        return "••••" + accountDetails.substring(Math.max(0, accountDetails.length() - 4));
    }

    /**
     * Round amount to specified decimal places
     */
    public BigDecimal round(int decimalPlace) {
        BigDecimal bd = new BigDecimal(Double.toString(amount));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Payout payout = (Payout) obj;
        return payoutId != null ? payoutId.equals(payout.payoutId) : payout.payoutId == null;
    }

    @Override
    public int hashCode() {
        return payoutId != null ? payoutId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Payout{" +
                "payoutId='" + payoutId + '\'' +
                ", period='" + period + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                '}';
    }
}