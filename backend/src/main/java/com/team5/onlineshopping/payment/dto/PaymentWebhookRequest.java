package com.team5.onlineshopping.payment.dto;

import com.team5.onlineshopping.common.enums.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PaymentWebhookRequest {

    @NotBlank(message = "Event ID is required")
    private String eventId;

    @NotBlank(message = "Payment reference is required")
    private String paymentReference;

    private String gatewayReference;

    @NotNull(message = "Payment status is required")
    private PaymentStatus paymentStatus; // CAPTURED or FAILED

    private String failureReason;

    @NotBlank(message = "Signature is required")
    private String signature; // HMAC or mock gateway verification token

    public PaymentWebhookRequest() {
    }

    public PaymentWebhookRequest(String eventId, String paymentReference, String gatewayReference,
                                 PaymentStatus paymentStatus, String failureReason, String signature) {
        this.eventId = eventId;
        this.paymentReference = paymentReference;
        this.gatewayReference = gatewayReference;
        this.paymentStatus = paymentStatus;
        this.failureReason = failureReason;
        this.signature = signature;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public void setGatewayReference(String gatewayReference) {
        this.gatewayReference = gatewayReference;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
