package com.team5.onlineshopping.refund.service;

import com.team5.onlineshopping.common.enums.RefundStatus;
import com.team5.onlineshopping.refund.dto.RefundAmountCalculation;
import com.team5.onlineshopping.refund.dto.RefundRequestDto;
import com.team5.onlineshopping.refund.dto.RefundResultDto;

/**
 * Interface contract for Refund initiation, tracking, and calculation.
 * <p>
 * Per Team 5 V2.0 Shared Architecture:
 * - Module owned / implemented by Member 1 (Owais) for financial & payment processing.
 * - Rashika (Member 2) must call this service rather than initiating payment gateway refunds directly:
 *   Returns & Fulfilment triggers return inspection, and when approved, calls {@code RefundService.initiateRefund(...)}.
 * - Guarantees idempotency via {@code idempotencyKey}.
 */
public interface RefundService {

    /**
     * Initiates a refund through payment gateway with idempotency support.
     *
     * @param request refund request containing orderItemId/returnRequestId, amount, idempotencyKey
     * @return RefundResultDto
     */
    RefundResultDto initiateRefund(RefundRequestDto request);

    /**
     * Gets status of an existing refund transaction by idempotency key.
     *
     * @param idempotencyKey unique idempotency key
     * @return current RefundStatus
     */
    RefundStatus getRefundStatus(String idempotencyKey);

    /**
     * Calculates the exact refundable amount for a returned item taking into account allocated discount.
     *
     * @param orderItemId      ID of order item
     * @param returnedQuantity quantity being returned
     * @return RefundAmountCalculation
     */
    RefundAmountCalculation calculateRefundAmount(Long orderItemId, int returnedQuantity);
}
