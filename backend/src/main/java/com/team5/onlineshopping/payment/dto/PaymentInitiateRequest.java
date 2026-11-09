package com.team5.onlineshopping.payment.dto;

import com.team5.onlineshopping.common.enums.PaymentMethod;

public class PaymentInitiateRequest {

    private PaymentMethod paymentMethod;
    private String idempotencyKey;
    private String simulatedOutcome; // OPTIONAL: "SUCCESS" or "FAIL" (for testing/demo)

    public PaymentInitiateRequest() {
    }

    public PaymentInitiateRequest(PaymentMethod paymentMethod, String idempotencyKey) {
        this.paymentMethod = paymentMethod;
        this.idempotencyKey = idempotencyKey;
    }

    public PaymentInitiateRequest(PaymentMethod paymentMethod, String idempotencyKey, String simulatedOutcome) {
        this.paymentMethod = paymentMethod;
        this.idempotencyKey = idempotencyKey;
        this.simulatedOutcome = simulatedOutcome;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getSimulatedOutcome() {
        return simulatedOutcome;
    }

    public void setSimulatedOutcome(String simulatedOutcome) {
        this.simulatedOutcome = simulatedOutcome;
    }
}
