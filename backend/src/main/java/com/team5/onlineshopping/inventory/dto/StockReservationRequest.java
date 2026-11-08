package com.team5.onlineshopping.inventory.dto;

import java.time.Instant;
import java.util.List;

public class StockReservationRequest {

    private String checkoutReference;
    private Long customerId;
    private List<ReservationItem> items;
    private Instant expiresAt;

    public StockReservationRequest() {
    }

    public StockReservationRequest(String checkoutReference, Long customerId, List<ReservationItem> items) {
        this.checkoutReference = checkoutReference;
        this.customerId = customerId;
        this.items = items;
    }

    public String getCheckoutReference() {
        return checkoutReference;
    }

    public void setCheckoutReference(String checkoutReference) {
        this.checkoutReference = checkoutReference;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public List<ReservationItem> getItems() {
        return items;
    }

    public void setItems(List<ReservationItem> items) {
        this.items = items;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
