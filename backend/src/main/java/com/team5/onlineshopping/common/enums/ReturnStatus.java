package com.team5.onlineshopping.common.enums;

/**
 * Return workflow states as defined in Version 2.0 Contract (Section 2.8 & 3.3).
 */
public enum ReturnStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    PICKUP_SCHEDULED,
    RECEIVED,
    INSPECTED,
    COMPLETED
}
