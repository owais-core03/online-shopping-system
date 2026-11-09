package com.team5.onlineshopping.checkout.controller;

import com.team5.onlineshopping.checkout.dto.CheckoutRequest;
import com.team5.onlineshopping.checkout.dto.CheckoutResponse;
import com.team5.onlineshopping.checkout.dto.CouponApplyRequest;
import com.team5.onlineshopping.checkout.dto.CouponApplyResponse;
import com.team5.onlineshopping.checkout.service.CheckoutService;
import com.team5.onlineshopping.common.dto.ApiResponse;
import com.team5.onlineshopping.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('CUSTOMER')")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/coupons/apply")
    public ResponseEntity<ApiResponse<CouponApplyResponse>> applyCoupon(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CouponApplyRequest request) {
        CouponApplyResponse response = checkoutService.validateCartCoupon(userDetails.getId(), request.getCouponCode());
        return ResponseEntity.ok(ApiResponse.success("Coupon validated", response));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CheckoutRequest request) {
        CheckoutResponse response = checkoutService.processCheckout(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Checkout completed successfully", response));
    }
}
