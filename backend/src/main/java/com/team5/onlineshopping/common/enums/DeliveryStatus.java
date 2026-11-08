package com.team5.onlineshopping.common.enums;

/**
 * Package-level delivery lifecycle for Shipment as defined in Version 2.0 Contract (Section 2.6 & 3.3).
 */
public enum DeliveryStatus {
    UNASSIGNED,
    ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERY_FAILED,
    RETURN_TO_SELLER,
    RETURNED_TO_SELLER,
    DELIVERED
}
