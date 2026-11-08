package com.team5.onlineshopping.order;

import com.team5.onlineshopping.common.enums.OrderItemStatus;
import com.team5.onlineshopping.common.enums.OrderStatus;
import com.team5.onlineshopping.order.entity.OrderItem;
import com.team5.onlineshopping.order.repository.OrderItemRepository;
import com.team5.onlineshopping.order.repository.OrderRepository;
import com.team5.onlineshopping.order.service.OrderStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderStatusService Aggregation & Precedence Tests")
class OrderStatusServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    private OrderStatusService orderStatusService;

    @BeforeEach
    void setUp() {
        orderStatusService = new OrderStatusService(orderRepository, orderItemRepository);
    }

    private OrderItem createItem(OrderItemStatus status) {
        OrderItem item = new OrderItem();
        item.setItemStatus(status);
        return item;
    }

    @Test
    @DisplayName("Empty items list returns CREATED")
    void emptyItems_returnsCreated() {
        assertThat(orderStatusService.calculateOrderStatus(Collections.emptyList())).isEqualTo(OrderStatus.CREATED);
        assertThat(orderStatusService.calculateOrderStatus(null)).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    @DisplayName("All items CANCELLED or REJECTED returns CANCELLED")
    void allCancelledOrRejected_returnsCancelled() {
        List<OrderItem> items = List.of(
                createItem(OrderItemStatus.CANCELLED),
                createItem(OrderItemStatus.REJECTED)
        );
        assertThat(orderStatusService.calculateOrderStatus(items)).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("All active items DELIVERED returns DELIVERED")
    void allActiveDelivered_returnsDelivered() {
        List<OrderItem> items = List.of(
                createItem(OrderItemStatus.DELIVERED),
                createItem(OrderItemStatus.DELIVERED)
        );
        assertThat(orderStatusService.calculateOrderStatus(items)).isEqualTo(OrderStatus.DELIVERED);

        // Even with a cancelled item, all non-cancelled delivered means DELIVERED
        List<OrderItem> withCancelled = List.of(
                createItem(OrderItemStatus.DELIVERED),
                createItem(OrderItemStatus.CANCELLED)
        );
        assertThat(orderStatusService.calculateOrderStatus(withCancelled)).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("All active items DISPATCHED returns SHIPPED")
    void allActiveDispatched_returnsShipped() {
        List<OrderItem> items = List.of(
                createItem(OrderItemStatus.DISPATCHED),
                createItem(OrderItemStatus.DISPATCHED)
        );
        assertThat(orderStatusService.calculateOrderStatus(items)).isEqualTo(OrderStatus.SHIPPED);

        List<OrderItem> withCancelled = List.of(
                createItem(OrderItemStatus.DISPATCHED),
                createItem(OrderItemStatus.CANCELLED)
        );
        assertThat(orderStatusService.calculateOrderStatus(withCancelled)).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("Precedence: PARTIALLY_DELIVERED wins over SHIPPED/CANCELLED/CONFIRMED")
    void partiallyDelivered_highestPrecedenceAmongPartials() {
        // One delivered, one dispatched, one cancelled -> PARTIALLY_DELIVERED wins
        List<OrderItem> items = List.of(
                createItem(OrderItemStatus.DELIVERED),
                createItem(OrderItemStatus.DISPATCHED),
                createItem(OrderItemStatus.CANCELLED)
        );
        assertThat(orderStatusService.calculateOrderStatus(items)).isEqualTo(OrderStatus.PARTIALLY_DELIVERED);

        // One delivered, one pending acceptance
        List<OrderItem> items2 = List.of(
                createItem(OrderItemStatus.DELIVERED),
                createItem(OrderItemStatus.PENDING_SELLER_ACCEPTANCE)
        );
        assertThat(orderStatusService.calculateOrderStatus(items2)).isEqualTo(OrderStatus.PARTIALLY_DELIVERED);
    }

    @Test
    @DisplayName("Precedence: PARTIALLY_SHIPPED wins over CANCELLED and CONFIRMED")
    void partiallyShipped_precedenceOverCancelledAndConfirmed() {
        // One dispatched, one processing, one cancelled
        List<OrderItem> items = List.of(
                createItem(OrderItemStatus.DISPATCHED),
                createItem(OrderItemStatus.PROCESSING),
                createItem(OrderItemStatus.CANCELLED)
        );
        assertThat(orderStatusService.calculateOrderStatus(items)).isEqualTo(OrderStatus.PARTIALLY_SHIPPED);
    }

    @Test
    @DisplayName("Precedence: PARTIALLY_CANCELLED wins over PARTIALLY_CONFIRMED")
    void partiallyCancelled_precedenceOverPartiallyConfirmed() {
        // One cancelled, one pending acceptance, one processing
        List<OrderItem> items = List.of(
                createItem(OrderItemStatus.CANCELLED),
                createItem(OrderItemStatus.PROCESSING),
                createItem(OrderItemStatus.PENDING_SELLER_ACCEPTANCE)
        );
        assertThat(orderStatusService.calculateOrderStatus(items)).isEqualTo(OrderStatus.PARTIALLY_CANCELLED);
    }

    @Test
    @DisplayName("Precedence: PARTIALLY_CONFIRMED when some accepted/processing and some pending")
    void partiallyConfirmed_whenSomeProcessingSomePending() {
        List<OrderItem> items = List.of(
                createItem(OrderItemStatus.PROCESSING),
                createItem(OrderItemStatus.PENDING_SELLER_ACCEPTANCE)
        );
        assertThat(orderStatusService.calculateOrderStatus(items)).isEqualTo(OrderStatus.PARTIALLY_CONFIRMED);
    }

    @Test
    @DisplayName("All items PROCESSING or PACKED returns PROCESSING")
    void allProcessingOrPacked_returnsProcessing() {
        List<OrderItem> items = List.of(
                createItem(OrderItemStatus.PROCESSING),
                createItem(OrderItemStatus.PACKED)
        );
        assertThat(orderStatusService.calculateOrderStatus(items)).isEqualTo(OrderStatus.PROCESSING);
    }
}
