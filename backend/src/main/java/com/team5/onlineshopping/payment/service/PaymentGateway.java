package com.team5.onlineshopping.payment.service;

import com.team5.onlineshopping.common.enums.PaymentMethod;
import com.team5.onlineshopping.common.enums.PaymentStatus;

import java.math.BigDecimal;

public interface PaymentGateway {

    record GatewayInitiateResult(
            String paymentReference,
            String gatewayReference,
            PaymentStatus initialStatus,
            String redirectUrl
    ) {}

    record GatewayVerifyResult(
            boolean verified,
            PaymentStatus status,
            String gatewayReference,
            String failureReason
    ) {}

    GatewayInitiateResult initiatePayment(Long orderId, String orderNumber, BigDecimal amount,
                                          PaymentMethod paymentMethod, String simulatedOutcome);

    GatewayVerifyResult verifyPayment(String paymentReference, String signature);

    String generateSignature(String paymentReference, PaymentStatus status);
}
