package com.team5.onlineshopping.coupon.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class CouponReservationResult {

    private boolean reserved;
    private String couponCode;
    private String checkoutReference;
    private BigDecimal discountAmount;
    private Instant expiresAt;
    private String message;

    public CouponReservationResult() {
    }

    public CouponReservationResult(boolean reserved, String couponCode, String checkoutReference,
                                   BigDecimal discountAmount, Instant expiresAt, String message) {
        this.reserved = reserved;
        this.couponCode = couponCode;
        this.checkoutReference = checkoutReference;
        this.discountAmount = discountAmount;
        this.expiresAt = expiresAt;
        this.message = message;
    }

    public boolean isReserved() {
        return reserved;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public String getCheckoutReference() {
        return checkoutReference;
    }

    public void setCheckoutReference(String checkoutReference) {
        this.checkoutReference = checkoutReference;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
