package com.team5.onlineshopping.inventory.dto;

public class StockAvailabilityResponse {

    private Long productId;
    private Integer availableStock;
    private Integer reservedStock;
    private boolean purchasable;

    public StockAvailabilityResponse() {
    }

    public StockAvailabilityResponse(Long productId, Integer availableStock, Integer reservedStock, boolean purchasable) {
        this.productId = productId;
        this.availableStock = availableStock;
        this.reservedStock = reservedStock;
        this.purchasable = purchasable;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public Integer getReservedStock() {
        return reservedStock;
    }

    public void setReservedStock(Integer reservedStock) {
        this.reservedStock = reservedStock;
    }

    public boolean isPurchasable() {
        return purchasable;
    }

    public void setPurchasable(boolean purchasable) {
        this.purchasable = purchasable;
    }
}
