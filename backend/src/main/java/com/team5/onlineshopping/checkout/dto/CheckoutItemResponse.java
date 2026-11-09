package com.team5.onlineshopping.checkout.dto;

import java.math.BigDecimal;

public class CheckoutItemResponse {

    private Long orderItemId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal grossAmount;
    private BigDecimal allocatedDiscount;
    private BigDecimal netPaidAmount;

    public CheckoutItemResponse() {
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public BigDecimal getAllocatedDiscount() {
        return allocatedDiscount;
    }

    public void setAllocatedDiscount(BigDecimal allocatedDiscount) {
        this.allocatedDiscount = allocatedDiscount;
    }

    public BigDecimal getNetPaidAmount() {
        return netPaidAmount;
    }

    public void setNetPaidAmount(BigDecimal netPaidAmount) {
        this.netPaidAmount = netPaidAmount;
    }
}
