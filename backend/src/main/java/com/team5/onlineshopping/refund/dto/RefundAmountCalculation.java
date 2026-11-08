package com.team5.onlineshopping.refund.dto;

import java.math.BigDecimal;

public class RefundAmountCalculation {

    private Long orderItemId;
    private Integer returnedQuantity;
    private BigDecimal unitPrice;
    private BigDecimal allocatedDiscount;
    private BigDecimal netRefundAmount;

    public RefundAmountCalculation() {
    }

    public RefundAmountCalculation(Long orderItemId, Integer returnedQuantity, BigDecimal unitPrice,
                                   BigDecimal allocatedDiscount, BigDecimal netRefundAmount) {
        this.orderItemId = orderItemId;
        this.returnedQuantity = returnedQuantity;
        this.unitPrice = unitPrice;
        this.allocatedDiscount = allocatedDiscount;
        this.netRefundAmount = netRefundAmount;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Integer getReturnedQuantity() {
        return returnedQuantity;
    }

    public void setReturnedQuantity(Integer returnedQuantity) {
        this.returnedQuantity = returnedQuantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getAllocatedDiscount() {
        return allocatedDiscount;
    }

    public void setAllocatedDiscount(BigDecimal allocatedDiscount) {
        this.allocatedDiscount = allocatedDiscount;
    }

    public BigDecimal getNetRefundAmount() {
        return netRefundAmount;
    }

    public void setNetRefundAmount(BigDecimal netRefundAmount) {
        this.netRefundAmount = netRefundAmount;
    }
}
