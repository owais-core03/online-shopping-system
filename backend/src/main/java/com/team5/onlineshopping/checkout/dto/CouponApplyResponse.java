package com.team5.onlineshopping.checkout.dto;

import java.math.BigDecimal;

public class CouponApplyResponse {

    private String couponCode;
    private String discountType;
    private BigDecimal discountAmount;
    private BigDecimal verifiedSubtotal;
    private BigDecimal finalPayable;

    public CouponApplyResponse() {
    }

    public CouponApplyResponse(String couponCode, String discountType, BigDecimal discountAmount, BigDecimal verifiedSubtotal, BigDecimal finalPayable) {
        this.couponCode = couponCode;
        this.discountType = discountType;
        this.discountAmount = discountAmount;
        this.verifiedSubtotal = verifiedSubtotal;
        this.finalPayable = finalPayable;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getVerifiedSubtotal() {
        return verifiedSubtotal;
    }

    public void setVerifiedSubtotal(BigDecimal verifiedSubtotal) {
        this.verifiedSubtotal = verifiedSubtotal;
    }

    public BigDecimal getFinalPayable() {
        return finalPayable;
    }

    public void setFinalPayable(BigDecimal finalPayable) {
        this.finalPayable = finalPayable;
    }
}
