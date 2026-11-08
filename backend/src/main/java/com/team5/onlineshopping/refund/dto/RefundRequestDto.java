package com.team5.onlineshopping.refund.dto;

import java.math.BigDecimal;

public class RefundRequestDto {

    private Long orderId;
    private Long orderItemId;
    private Long returnRequestId;
    private BigDecimal amount;
    private String reason;
    private String idempotencyKey;

    public RefundRequestDto() {
    }

    public RefundRequestDto(Long orderId, Long orderItemId, Long returnRequestId,
                            BigDecimal amount, String reason, String idempotencyKey) {
        this.orderId = orderId;
        this.orderItemId = orderItemId;
        this.returnRequestId = returnRequestId;
        this.amount = amount;
        this.reason = reason;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Long getReturnRequestId() {
        return returnRequestId;
    }

    public void setReturnRequestId(Long returnRequestId) {
        this.returnRequestId = returnRequestId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
