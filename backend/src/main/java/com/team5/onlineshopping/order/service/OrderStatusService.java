package com.team5.onlineshopping.order.service;

import com.team5.onlineshopping.common.enums.OrderItemStatus;
import com.team5.onlineshopping.common.enums.OrderStatus;
import com.team5.onlineshopping.order.entity.Order;
import com.team5.onlineshopping.order.entity.OrderItem;
import com.team5.onlineshopping.order.repository.OrderItemRepository;
import com.team5.onlineshopping.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service responsible for computing aggregate Order status from individual OrderItem statuses.
 * <p>
 * According to Version 2.0 Shared Architecture & API Contract:
 * - Aggregation precedence must be implemented centrally in OrderStatusService and covered by tests.
 * - If more than one partial condition is true, use this priority:
 *   PARTIALLY_DELIVERED, PARTIALLY_SHIPPED, PARTIALLY_CANCELLED, PARTIALLY_CONFIRMED.
 * - Payment failure/cancellation is handled separately by payment_status and does not fabricate a fulfilment status.
 */
@Service
public class OrderStatusService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderStatusService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    /**
     * Calculates the derived OrderStatus from a collection of order items.
     *
     * @param items the items of the order
     * @return the calculated OrderStatus, or OrderStatus.CREATED if empty
     */
    public OrderStatus calculateOrderStatus(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return OrderStatus.CREATED;
        }

        int total = items.size();
        int pendingAcceptanceCount = 0;
        int processingCount = 0;
        int packedCount = 0;
        int dispatchedCount = 0;
        int deliveredCount = 0;
        int cancelledOrRejectedCount = 0;

        for (OrderItem item : items) {
            OrderItemStatus status = item.getItemStatus();
            if (status == null) {
                continue;
            }
            switch (status) {
                case PENDING_SELLER_ACCEPTANCE -> pendingAcceptanceCount++;
                case PROCESSING -> processingCount++;
                case PACKED -> packedCount++;
                case DISPATCHED -> dispatchedCount++;
                case DELIVERED -> deliveredCount++;
                case CANCELLED, REJECTED -> cancelledOrRejectedCount++;
            }
        }

        // 1. All items cancelled or rejected
        if (cancelledOrRejectedCount == total) {
            return OrderStatus.CANCELLED;
        }

        int activeItems = total - cancelledOrRejectedCount;

        // 2. All active items delivered
        if (deliveredCount > 0 && deliveredCount == activeItems) {
            return OrderStatus.DELIVERED;
        }

        // 3. All active items shipped / dispatched
        if (dispatchedCount > 0 && dispatchedCount == activeItems) {
            return OrderStatus.SHIPPED;
        }

        // 4. Partial precedence check per V2 Spec:
        // Priority: PARTIALLY_DELIVERED, PARTIALLY_SHIPPED, PARTIALLY_CANCELLED, PARTIALLY_CONFIRMED

        // PARTIALLY_DELIVERED: At least one item delivered and at least one non-cancelled item remains undelivered
        if (deliveredCount > 0 && deliveredCount < activeItems) {
            return OrderStatus.PARTIALLY_DELIVERED;
        }

        // PARTIALLY_SHIPPED: At least one item/shipment dispatched and at least one eligible item not yet dispatched
        if (dispatchedCount > 0 && dispatchedCount < activeItems) {
            return OrderStatus.PARTIALLY_SHIPPED;
        }

        // PARTIALLY_CANCELLED: At least one item cancelled/rejected and at least one item remains active
        if (cancelledOrRejectedCount > 0 && activeItems > 0) {
            return OrderStatus.PARTIALLY_CANCELLED;
        }

        // PARTIALLY_CONFIRMED: At least one item is PROCESSING/PACKED and at least one item is still awaiting decision
        if ((processingCount > 0 || packedCount > 0) && pendingAcceptanceCount > 0) {
            return OrderStatus.PARTIALLY_CONFIRMED;
        }

        // 5. If all active items are accepted and being processed (PROCESSING / PACKED)
        if ((processingCount + packedCount) == activeItems && activeItems > 0) {
            return OrderStatus.PROCESSING;
        }

        // 6. Default if all items are still pending seller acceptance
        if (pendingAcceptanceCount == activeItems && activeItems > 0) {
            return OrderStatus.PROCESSING; // Order payment captured, waiting on seller actions
        }

        return OrderStatus.PROCESSING;
    }

    /**
     * Recalculates and persists the order's status based on current order items.
     *
     * @param orderId ID of the order to recalculate
     * @return updated Order
     */
    @Transactional
    public Order recalculateAndSave(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        OrderStatus derivedStatus = calculateOrderStatus(items);
        order.setOrderStatus(derivedStatus);
        return orderRepository.save(order);
    }
}
