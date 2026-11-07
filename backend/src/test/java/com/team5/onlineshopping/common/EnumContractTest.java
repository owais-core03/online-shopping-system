package com.team5.onlineshopping.common;

import com.team5.onlineshopping.common.enums.DeliveryStatus;
import com.team5.onlineshopping.common.enums.DiscountType;
import com.team5.onlineshopping.common.enums.OrderItemStatus;
import com.team5.onlineshopping.common.enums.OrderStatus;
import com.team5.onlineshopping.common.enums.PaymentMethod;
import com.team5.onlineshopping.common.enums.PaymentStatus;
import com.team5.onlineshopping.common.enums.RefundStatus;
import com.team5.onlineshopping.common.enums.ReservationStatus;
import com.team5.onlineshopping.common.enums.ReturnStatus;
import com.team5.onlineshopping.common.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Lifecycle enum contract tests")
class EnumContractTest {

    // ─── Role ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Role contains CUSTOMER, SELLER, DELIVERY_AGENT, ADMIN")
    void role_hasAllExpectedValues() {
        EnumSet<Role> roles = EnumSet.allOf(Role.class);
        assertThat(roles).contains(Role.CUSTOMER, Role.SELLER, Role.DELIVERY_AGENT, Role.ADMIN);
    }

    // ─── OrderStatus ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("OrderStatus contains all lifecycle states")
    void orderStatus_hasAllExpectedValues() {
        EnumSet<OrderStatus> statuses = EnumSet.allOf(OrderStatus.class);
        assertThat(statuses).contains(
                OrderStatus.CREATED,
                OrderStatus.PAYMENT_PENDING,
                OrderStatus.PROCESSING,
                OrderStatus.CONFIRMED,
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERED,
                OrderStatus.CANCELLED,
                OrderStatus.COMPLETED
        );
    }

    // ─── OrderItemStatus ──────────────────────────────────────────────────────

    @Test
    @DisplayName("OrderItemStatus contains PROCESSING state")
    void orderItemStatus_containsProcessing() {
        EnumSet<OrderItemStatus> statuses = EnumSet.allOf(OrderItemStatus.class);
        assertThat(statuses).contains(OrderItemStatus.PROCESSING);
    }

    @Test
    @DisplayName("OrderItemStatus does NOT contain RESERVED (reservation belongs to ReservationStatus)")
    void orderItemStatus_doesNotContainReserved() {
        for (OrderItemStatus s : OrderItemStatus.values()) {
            assertThat(s.name()).isNotEqualTo("RESERVED");
        }
    }

    // ─── ReservationStatus ────────────────────────────────────────────────────

    @Test
    @DisplayName("ReservationStatus contains ACTIVE and RELEASED states")
    void reservationStatus_hasExpectedValues() {
        EnumSet<ReservationStatus> statuses = EnumSet.allOf(ReservationStatus.class);
        assertThat(statuses).contains(ReservationStatus.ACTIVE, ReservationStatus.RELEASED, ReservationStatus.EXPIRED);
    }

    // ─── PaymentStatus ────────────────────────────────────────────────────────

    @Test
    @DisplayName("PaymentStatus contains PENDING, CAPTURED and FAILED")
    void paymentStatus_hasExpectedValues() {
        EnumSet<PaymentStatus> statuses = EnumSet.allOf(PaymentStatus.class);
        assertThat(statuses).contains(PaymentStatus.PENDING, PaymentStatus.CAPTURED, PaymentStatus.FAILED);
    }

    // ─── PaymentMethod ────────────────────────────────────────────────────────

    @Test
    @DisplayName("PaymentMethod has at least one value")
    void paymentMethod_hasValues() {
        assertThat(PaymentMethod.values()).isNotEmpty();
    }

    // ─── DeliveryStatus ───────────────────────────────────────────────────────

    @Test
    @DisplayName("DeliveryStatus contains DELIVERY_FAILED, RETURN_TO_SELLER, RETURNED_TO_SELLER")
    void deliveryStatus_hasReturnValues() {
        EnumSet<DeliveryStatus> statuses = EnumSet.allOf(DeliveryStatus.class);
        assertThat(statuses).contains(
                DeliveryStatus.DELIVERY_FAILED,
                DeliveryStatus.RETURN_TO_SELLER,
                DeliveryStatus.RETURNED_TO_SELLER
        );
    }

    // ─── ReturnStatus ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("ReturnStatus has at least one value")
    void returnStatus_hasValues() {
        assertThat(ReturnStatus.values()).isNotEmpty();
    }

    // ─── RefundStatus ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("RefundStatus has at least one value")
    void refundStatus_hasValues() {
        assertThat(RefundStatus.values()).isNotEmpty();
    }

    // ─── DiscountType ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("DiscountType has at least one value")
    void discountType_hasValues() {
        assertThat(DiscountType.values()).isNotEmpty();
    }

    // ─── Enum.name() contract ─────────────────────────────────────────────────

    @Test
    @DisplayName("All Role names are uppercase only")
    void roleNames_areUppercase() {
        for (Role role : Role.values()) {
            assertThat(role.name()).isEqualTo(role.name().toUpperCase());
        }
    }

    @Test
    @DisplayName("All OrderStatus names are uppercase only")
    void orderStatusNames_areUppercase() {
        for (OrderStatus status : OrderStatus.values()) {
            assertThat(status.name()).isEqualTo(status.name().toUpperCase());
        }
    }
}
