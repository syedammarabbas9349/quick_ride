package com.example.quickride.models;

import androidx.annotation.Keep;
import com.example.quickride.R;

/**
 * Payment Method model for JazzCash, EasyPaisa, and Cash
 *
 * Supports:
 * - JazzCash: Mobile number account
 * - EasyPaisa: Mobile number account
 * - Cash: Simple cash on delivery
 */
@Keep
public class PaymentMethod {

    private String id;
    private String type;           // "jazzcash", "easypaisa", "cash"
    private String name;            // Display name (e.g., "JazzCash", "EasyPaisa")
    private String mobileNumber;    // For JazzCash/EasyPaisa
    private String accountHolderName; // Name on account
    private String details;         // Additional details
    private boolean isDefault;      // Default payment method
    private long addedAt;           // Timestamp when added

    // For cash payments only
    private boolean cashOnDelivery; // True for cash payments

    // Transaction limits (optional)
    private double dailyLimit;
    private double monthlyLimit;

    // Status
    private boolean isVerified;      // If mobile number is verified
    private boolean isActive;        // If payment method is active

    /**
     * Empty constructor required for Firebase
     */
    public PaymentMethod() {}

    /**
     * Constructor for JazzCash/EasyPaisa
     */
    public PaymentMethod(String id, String type, String mobileNumber, String accountHolderName) {
        this.id = id;
        this.type = type;
        this.mobileNumber = mobileNumber;
        this.accountHolderName = accountHolderName;
        this.name = type.equals("jazzcash") ? "JazzCash" : "EasyPaisa";
        this.isDefault = false;
        this.isVerified = false;
        this.isActive = true;
        this.addedAt = System.currentTimeMillis();
    }

    /**
     * Constructor for Cash
     */
    public PaymentMethod(String id) {
        this.id = id;
        this.type = "cash";
        this.name = "Cash";
        this.cashOnDelivery = true;
        this.isDefault = false;
        this.isActive = true;
        this.addedAt = System.currentTimeMillis();
    }

    // ==================== GETTERS ====================

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public String getDetails() {
        return details;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public long getAddedAt() {
        return addedAt;
    }

    public boolean isCashOnDelivery() {
        return cashOnDelivery;
    }

    public double getDailyLimit() {
        return dailyLimit;
    }

    public double getMonthlyLimit() {
        return monthlyLimit;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public boolean isActive() {
        return isActive;
    }

    /**
     * Get formatted mobile number (e.g., 03XX-XXXXXXX)
     */
    public String getFormattedMobileNumber() {
        if (mobileNumber == null || mobileNumber.isEmpty()) {
            return "";
        }

        // Format: 03XX-XXXXXXX
        if (mobileNumber.length() == 11) {
            return mobileNumber.substring(0, 4) + "-" + mobileNumber.substring(4);
        } else if (mobileNumber.length() == 10) {
            return "0" + mobileNumber.substring(0, 3) + "-" + mobileNumber.substring(3);
        }
        return mobileNumber;
    }

    /**
     * Get masked mobile number (e.g., 03XX-XXX-XXXX)
     */
    public String getMaskedMobileNumber() {
        if (mobileNumber == null || mobileNumber.length() < 7) {
            return mobileNumber;
        }

        String formatted = getFormattedMobileNumber();
        if (formatted.length() > 8) {
            return formatted.substring(0, 5) + "••••" + formatted.substring(formatted.length() - 4);
        }
        return formatted;
    }

    // ==================== SETTERS ====================

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
        if ("jazzcash".equals(type)) {
            this.name = "JazzCash";
        } else if ("easypaisa".equals(type)) {
            this.name = "EasyPaisa";
        } else if ("cash".equals(type)) {
            this.name = "Cash";
            this.cashOnDelivery = true;
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void setAddedAt(long addedAt) {
        this.addedAt = addedAt;
    }

    public void setCashOnDelivery(boolean cashOnDelivery) {
        this.cashOnDelivery = cashOnDelivery;
    }

    public void setDailyLimit(double dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public void setMonthlyLimit(double monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Check if this is a JazzCash payment method
     */
    public boolean isJazzCash() {
        return "jazzcash".equals(type);
    }

    /**
     * Check if this is an EasyPaisa payment method
     */
    public boolean isEasyPaisa() {
        return "easypaisa".equals(type);
    }

    /**
     * Check if this is a Cash payment method
     */
    public boolean isCash() {
        return "cash".equals(type);
    }

    /**
     * Get icon resource ID based on payment type
     */
    public int getIconResource() {
        switch (type) {
            case "jazzcash":
                return R.drawable.ic_jazzcash;
            case "easypaisa":
                return R.drawable.ic_easypaisa;
            case "cash":
                return R.drawable.ic_cash;
            default:
                return R.drawable.ic_credit_card;
        }
    }

    /**
     * Get description for display
     */
    public String getDisplayDescription() {
        if (isCash()) {
            return "Pay with cash at dropoff";
        } else if (isJazzCash() || isEasyPaisa()) {
            return getMaskedMobileNumber() + " • " + accountHolderName;
        }
        return details != null ? details : "";
    }

    /**
     * Get short description for display
     */
    public String getShortDescription() {
        if (isCash()) {
            return "Cash";
        } else if (isJazzCash() || isEasyPaisa()) {
            return getFormattedMobileNumber();
        }
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        PaymentMethod that = (PaymentMethod) obj;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "PaymentMethod{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", mobileNumber='" + getMaskedMobileNumber() + '\'' +
                ", accountHolderName='" + accountHolderName + '\'' +
                ", isDefault=" + isDefault +
                '}';
    }
}