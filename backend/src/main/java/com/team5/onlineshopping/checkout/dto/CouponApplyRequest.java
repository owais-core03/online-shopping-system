package com.team5.onlineshopping.checkout.dto;

import java.math.BigDecimal;

public class CouponApplyRequest {

    private String couponCode;

    public CouponApplyRequest() {
    }

    public CouponApplyRequest(String couponCode) {
        this.couponCode = couponCode;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
}
