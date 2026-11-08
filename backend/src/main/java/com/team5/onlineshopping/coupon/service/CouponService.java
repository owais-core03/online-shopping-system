package com.team5.onlineshopping.coupon.service;

import com.team5.onlineshopping.coupon.dto.CouponReservationResult;
import com.team5.onlineshopping.coupon.dto.CouponValidationResult;

import java.math.BigDecimal;

/**
 * Interface contract for Coupon validation, reservation, and usage.
 * <p>
 * Per Team 5 V2.0 Shared Architecture:
 * - Module owned / implemented by Rashika (Member 2).
 * - Contract defined here so Member 1's checkout flow can cleanly interact with coupon operations.
 * - Rules:
 *   - 15-minute temporary reservation tied to checkoutReference.
 *   - Final commit occurs only upon verified payment.
 *   - Release upon checkout cancellation or payment failure.
 */
public interface CouponService {

    /**
     * Validates if a coupon code can be applied for the customer and order total.
     *
     * @param couponCode  code entered by customer
     * @param customerId  ID of customer
     * @param orderTotal  total before coupon discount
     * @return CouponValidationResult
     */
    CouponValidationResult validateCoupon(String couponCode, Long customerId, BigDecimal orderTotal);

    /**
     * Holds a coupon reservation for 15 minutes during checkout.
     *
     * @param couponCode        code entered by customer
     * @param customerId        ID of customer
     * @param checkoutReference unique checkout reference
     * @return CouponReservationResult
     */
    CouponReservationResult reserveCoupon(String couponCode, Long customerId, String checkoutReference);

    /**
     * Commits coupon usage upon successful payment confirmation.
     *
     * @param checkoutReference unique checkout reference
     */
    void confirmCouponUsage(String checkoutReference);

    /**
     * Releases coupon hold upon expired or cancelled checkout.
     *
     * @param checkoutReference unique checkout reference
     */
    void releaseCouponReservation(String checkoutReference);
}
