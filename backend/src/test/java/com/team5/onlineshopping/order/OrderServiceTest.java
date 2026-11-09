package com.team5.onlineshopping.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.onlineshopping.common.enums.OrderStatus;
import com.team5.onlineshopping.common.enums.PaymentMethod;
import com.team5.onlineshopping.common.enums.PaymentStatus;
import com.team5.onlineshopping.common.enums.Role;
import com.team5.onlineshopping.common.exception.ResourceNotFoundException;
import com.team5.onlineshopping.order.dto.OrderDetailsResponse;
import com.team5.onlineshopping.order.entity.Order;
import com.team5.onlineshopping.order.entity.OrderItem;
import com.team5.onlineshopping.order.repository.OrderRepository;
import com.team5.onlineshopping.order.service.OrderServiceImpl;
import com.team5.onlineshopping.product.entity.Product;
import com.team5.onlineshopping.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OrderServiceImpl orderService;

    private User customer;
    private User otherCustomer;
    private Order order;

    @BeforeEach
    void setUp() {
        customer = new User("customer@test.com", "pass123", "Customer One", "9876543210", Role.CUSTOMER);
        customer.setId(10L);

        otherCustomer = new User("other@test.com", "pass123", "Other Customer", "9876543211", Role.CUSTOMER);
        otherCustomer.setId(20L);

        order = new Order(
                "ORD-TEST-1",
                customer,
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                new BigDecimal("40.00"),
                new BigDecimal("1040.00"),
                PaymentMethod.CREDIT_CARD,
                "{\"recipientName\":\"Customer One\",\"phoneNumber\":\"9876543210\",\"streetLine1\":\"123 Main\",\"city\":\"Bengaluru\",\"state\":\"Karnataka\",\"postalCode\":\"560001\",\"country\":\"India\"}"
        );
        order.setId(101L);
        order.setOrderStatus(OrderStatus.PAYMENT_PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);

        Product product = new Product(customer, "Laptop Sleeve", "TechCo", "Accessories", "Description", null, new BigDecimal("1000.00"), 10);
        product.setId(50L);

        OrderItem item = new OrderItem(order, product, customer, 1, new BigDecimal("1000.00"), BigDecimal.ZERO);
        item.setId(201L);
        order.addItem(item);
    }

    @Test
    @DisplayName("getCustomerOrders returns list of mapped orders")
    void getCustomerOrders_returnsList() {
        when(orderRepository.findByCustomerId(10L)).thenReturn(List.of(order));

        List<OrderDetailsResponse> result = orderService.getCustomerOrders(10L);

        assertThat(result).hasSize(1);
        OrderDetailsResponse resp = result.get(0);
        assertThat(resp.getId()).isEqualTo(101L);
        assertThat(resp.getOrderNumber()).isEqualTo("ORD-TEST-1");
        assertThat(resp.getTotalAmount()).isEqualByComparingTo("1040.00");
        assertThat(resp.getDeliveryAddress()).isNotNull();
        assertThat(resp.getDeliveryAddress().getCity()).isEqualTo("Bengaluru");
        assertThat(resp.getItems()).hasSize(1);
        assertThat(resp.getItems().get(0).getProductName()).isEqualTo("Laptop Sleeve");
    }

    @Test
    @DisplayName("getCustomerOrderById returns order when customer is owner")
    void getCustomerOrderById_ownerMatches_returnsOrder() {
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));

        OrderDetailsResponse result = orderService.getCustomerOrderById(10L, 101L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(101L);
        assertThat(result.getCustomerId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getCustomerOrderById throws ResourceNotFoundException when accessing another customer's order")
    void getCustomerOrderById_differentCustomer_throws404() {
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getCustomerOrderById(20L, 101L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with id: 101");
    }

    @Test
    @DisplayName("getCustomerOrderById throws ResourceNotFoundException when order does not exist")
    void getCustomerOrderById_notFound_throws404() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getCustomerOrderById(10L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with id: 999");
    }
}
