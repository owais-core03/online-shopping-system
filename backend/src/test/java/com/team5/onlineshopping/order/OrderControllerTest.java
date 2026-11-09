package com.team5.onlineshopping.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.onlineshopping.checkout.dto.AddressDto;
import com.team5.onlineshopping.common.enums.OrderStatus;
import com.team5.onlineshopping.common.enums.PaymentMethod;
import com.team5.onlineshopping.common.enums.PaymentStatus;
import com.team5.onlineshopping.common.enums.Role;
import com.team5.onlineshopping.common.exception.ResourceNotFoundException;
import com.team5.onlineshopping.order.controller.OrderController;
import com.team5.onlineshopping.order.dto.OrderDetailsResponse;
import com.team5.onlineshopping.order.dto.OrderItemResponse;
import com.team5.onlineshopping.order.service.OrderService;
import com.team5.onlineshopping.security.CustomUserDetails;
import com.team5.onlineshopping.security.JwtService;
import com.team5.onlineshopping.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    value = OrderController.class,
    excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@Import(com.team5.onlineshopping.config.SecurityConfig.class)
@DisplayName("OrderController Slice Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private CustomUserDetails customerDetails;
    private OrderDetailsResponse sampleOrder;

    @BeforeEach
    void setUp() {
        User user = new User("customer@test.com", "pass123", "Customer One", "9876543210", Role.CUSTOMER);
        user.setId(10L);
        customerDetails = new CustomUserDetails(user);

        sampleOrder = new OrderDetailsResponse();
        sampleOrder.setId(101L);
        sampleOrder.setOrderNumber("ORD-20261110-101");
        sampleOrder.setCustomerId(10L);
        sampleOrder.setSubtotalAmount(new BigDecimal("1500.00"));
        sampleOrder.setDiscountAmount(BigDecimal.ZERO);
        sampleOrder.setShippingFee(new BigDecimal("50.00"));
        sampleOrder.setTotalAmount(new BigDecimal("1550.00"));
        sampleOrder.setOrderStatus(OrderStatus.PAYMENT_PENDING);
        sampleOrder.setPaymentStatus(PaymentStatus.PENDING);
        sampleOrder.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        sampleOrder.setCreatedAt(Instant.now());

        AddressDto addr = new AddressDto();
        addr.setRecipientName("Customer One");
        addr.setPhoneNumber("9876543210");
        addr.setStreetLine1("123 Market St");
        addr.setCity("Bengaluru");
        addr.setState("Karnataka");
        addr.setPostalCode("560001");
        addr.setCountry("India");
        sampleOrder.setDeliveryAddress(addr);

        OrderItemResponse item = new OrderItemResponse(
                1L, 201L, "Wireless Mouse", 2, new BigDecimal("750.00"),
                new BigDecimal("1500.00"), BigDecimal.ZERO, new BigDecimal("1500.00"), "PENDING_SELLER_ACCEPTANCE"
        );
        sampleOrder.setItems(List.of(item));
    }

    private void authenticateAsCustomer() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(customerDetails, null, customerDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GET /api/orders without authentication returns 403 Forbidden")
    void getOrders_unauthenticated_returns403() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/orders with customer principal returns list of customer orders")
    void getOrders_authenticatedCustomer_returnsOrders() throws Exception {
        authenticateAsCustomer();
        when(orderService.getCustomerOrders(eq(10L))).thenReturn(List.of(sampleOrder));

        mockMvc.perform(get("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(101L))
                .andExpect(jsonPath("$.data[0].orderNumber").value("ORD-20261110-101"))
                .andExpect(jsonPath("$.data[0].totalAmount").value(1550.00))
                .andExpect(jsonPath("$.data[0].items[0].productName").value("Wireless Mouse"));
    }

    @Test
    @DisplayName("GET /api/orders/{id} returns single order details for owner")
    void getOrderById_ownerCustomer_returnsOrderDetails() throws Exception {
        authenticateAsCustomer();
        when(orderService.getCustomerOrderById(eq(10L), eq(101L))).thenReturn(sampleOrder);

        mockMvc.perform(get("/api/orders/101")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101L))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-20261110-101"))
                .andExpect(jsonPath("$.data.deliveryAddress.city").value("Bengaluru"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));
    }

    @Test
    @DisplayName("GET /api/orders/{id} for non-existent order returns 404")
    void getOrderById_notFound_returns404() throws Exception {
        authenticateAsCustomer();
        when(orderService.getCustomerOrderById(eq(10L), eq(999L)))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 999"));

        mockMvc.perform(get("/api/orders/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/orders/{id} for order belonging to another customer throws ResourceNotFoundException (safe 404)")
    void getOrderById_differentCustomer_returns404Safe() throws Exception {
        authenticateAsCustomer();
        when(orderService.getCustomerOrderById(eq(10L), eq(202L)))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 202"));

        mockMvc.perform(get("/api/orders/202")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
