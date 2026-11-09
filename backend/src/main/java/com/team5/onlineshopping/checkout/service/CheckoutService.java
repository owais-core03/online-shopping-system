package com.team5.onlineshopping.checkout.service;

import com.team5.onlineshopping.checkout.dto.CheckoutRequest;
import com.team5.onlineshopping.checkout.dto.CheckoutResponse;
import com.team5.onlineshopping.checkout.dto.CouponApplyResponse;

public interface CheckoutService {

    CouponApplyResponse validateCartCoupon(Long customerId, String couponCode);

    CheckoutResponse processCheckout(Long customerId, CheckoutRequest request);
}
