package com.team5.onlineshopping.payment.service;

import com.team5.onlineshopping.payment.dto.PaymentInitiateRequest;
import com.team5.onlineshopping.payment.dto.PaymentInitiateResponse;
import com.team5.onlineshopping.payment.dto.PaymentStatusResponse;
import com.team5.onlineshopping.payment.dto.PaymentWebhookRequest;

public interface PaymentService {

    PaymentInitiateResponse initiatePayment(Long customerId, Long orderId, PaymentInitiateRequest request);

    PaymentStatusResponse handlePaymentWebhook(PaymentWebhookRequest webhookRequest);

    PaymentStatusResponse getPaymentStatus(Long customerId, Long orderId);

    PaymentStatusResponse getPaymentByReference(String paymentReference);
}
