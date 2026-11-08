package com.team5.onlineshopping.common.enums;

/**
 * Lifecycle status of an individual OrderItem as defined in Version 2.0 Contract (Section 2.5 & 3.2).
 */
public enum OrderItemStatus {
    PENDING_SELLER_ACCEPTANCE,
    PROCESSING,
    REJECTED,
    PACKED,
    DISPATCHED,
    DELIVERED,
    CANCELLED
}
