package com.team5.onlineshopping.refund.dto;

import com.team5.onlineshopping.common.enums.RefundStatus;

import java.math.BigDecimal;
import java.time.Instant;

public class RefundResultDto {

    private String refundCode;
    private String idempotencyKey;
    private RefundStatus status;
    private BigDecimal amount;
    private String gatewayReference;
    private Instant processedAt;

    public RefundResultDto() {
    }

    public RefundResultDto(String refundCode, String idempotencyKey, RefundStatus status,
                           BigDecimal amount, String gatewayReference, Instant processedAt) {
        this.refundCode = refundCode;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.amount = amount;
        this.gatewayReference = gatewayReference;
        this.processedAt = processedAt;
    }

    public String getRefundCode() {
        return refundCode;
    }

    public void setRefundCode(String refundCode) {
        this.refundCode = refundCode;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public void setStatus(RefundStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public void setGatewayReference(String gatewayReference) {
        this.gatewayReference = gatewayReference;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
