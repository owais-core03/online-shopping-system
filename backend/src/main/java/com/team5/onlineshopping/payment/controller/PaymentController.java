package com.team5.onlineshopping.payment.controller;

import com.team5.onlineshopping.common.dto.ApiResponse;
import com.team5.onlineshopping.payment.dto.PaymentInitiateRequest;
import com.team5.onlineshopping.payment.dto.PaymentInitiateResponse;
import com.team5.onlineshopping.payment.dto.PaymentStatusResponse;
import com.team5.onlineshopping.payment.dto.PaymentWebhookRequest;
import com.team5.onlineshopping.payment.service.PaymentService;
import com.team5.onlineshopping.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{orderId}/initiate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PaymentInitiateResponse>> initiatePayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId,
            @RequestBody(required = false) PaymentInitiateRequest request) {
        PaymentInitiateResponse response = paymentService.initiatePayment(userDetails.getId(), orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Payment initiated successfully", response));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> handlePaymentWebhook(
            @Valid @RequestBody PaymentWebhookRequest request) {
        PaymentStatusResponse response = paymentService.handlePaymentWebhook(request);
        return ResponseEntity.ok(ApiResponse.success("Payment webhook processed", response));
    }

    @GetMapping("/{orderId}/status")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId) {
        PaymentStatusResponse response = paymentService.getPaymentStatus(userDetails.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Payment status retrieved", response));
    }
}
