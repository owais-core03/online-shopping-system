package com.team5.onlineshopping.inventory.service;

import com.team5.onlineshopping.inventory.dto.StockAvailabilityResponse;
import com.team5.onlineshopping.inventory.dto.StockReservationRequest;
import com.team5.onlineshopping.inventory.dto.StockReservationResult;

/**
 * Shared interface for Inventory management and stock reservation contract.
 * <p>
 * Per Team 5 V2.0 Shared Architecture:
 * - Owned / implemented by Rashika (Member 2).
 * - Contract defined here so Member 1's checkout/order services can compile and bind to it cleanly.
 * - Manages available_stock and reserved_stock arithmetic:
 *   - On reservation: available decreases, reserved increases.
 *   - On payment commit: reserved decreases, stock decrement committed.
 *   - On expiry/release: reserved decreases, available increases.
 */
public interface InventoryService {

    /**
     * Checks current available and reserved stock for a product.
     *
     * @param productId ID of product
     * @param quantity  requested quantity
     * @return StockAvailabilityResponse
     */
    StockAvailabilityResponse checkAvailability(Long productId, int quantity);

    /**
     * Reserves stock for items in checkout.
     *
     * @param request reservation request with checkoutReference and items
     * @return StockReservationResult
     */
    StockReservationResult reserveStock(StockReservationRequest request);

    /**
     * Confirms and commits stock reservation upon successful payment.
     *
     * @param checkoutReference unique checkout reference
     */
    void confirmReservation(String checkoutReference);

    /**
     * Releases an active stock reservation (e.g. user cancels checkout or payment fails).
     *
     * @param checkoutReference unique checkout reference
     */
    void releaseReservation(String checkoutReference);

    /**
     * Background/scheduled task to expire stale reservations exceeding TTL.
     */
    void expireStaleReservations();
}
