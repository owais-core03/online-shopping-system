package com.team5.onlineshopping.inventory.dto;

import com.team5.onlineshopping.common.enums.ReservationStatus;

import java.time.Instant;
import java.util.List;

public class StockReservationResult {

    private String checkoutReference;
    private String reservationCode;
    private ReservationStatus status;
    private List<ReservationItem> reservedItems;
    private Instant expiresAt;

    public StockReservationResult() {
    }

    public StockReservationResult(String checkoutReference, String reservationCode,
                                  ReservationStatus status, List<ReservationItem> reservedItems,
                                  Instant expiresAt) {
        this.checkoutReference = checkoutReference;
        this.reservationCode = reservationCode;
        this.status = status;
        this.reservedItems = reservedItems;
        this.expiresAt = expiresAt;
    }

    public String getCheckoutReference() {
        return checkoutReference;
    }

    public void setCheckoutReference(String checkoutReference) {
        this.checkoutReference = checkoutReference;
    }

    public String getReservationCode() {
        return reservationCode;
    }

    public void setReservationCode(String reservationCode) {
        this.reservationCode = reservationCode;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public List<ReservationItem> getReservedItems() {
        return reservedItems;
    }

    public void setReservedItems(List<ReservationItem> reservedItems) {
        this.reservedItems = reservedItems;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
