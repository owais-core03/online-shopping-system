package com.team5.onlineshopping.coupon.dto;

import java.math.BigDecimal;

public class CouponValidationResult {

    private boolean valid;
    private String couponCode;
    private BigDecimal discountAmount;
    private String message;

    public CouponValidationResult() {
    }

    public CouponValidationResult(boolean valid, String couponCode, BigDecimal discountAmount, String message) {
        this.valid = valid;
        this.couponCode = couponCode;
        this.discountAmount = discountAmount;
        this.message = message;
    }

    public static CouponValidationResult valid(String couponCode, BigDecimal discountAmount) {
        return new CouponValidationResult(true, couponCode, discountAmount, "Coupon is valid");
    }

    public static CouponValidationResult invalid(String couponCode, String message) {
        return new CouponValidationResult(false, couponCode, BigDecimal.ZERO, message);
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
