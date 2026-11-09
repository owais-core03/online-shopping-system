package com.team5.onlineshopping.checkout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.onlineshopping.checkout.controller.CheckoutController;
import com.team5.onlineshopping.checkout.dto.AddressDto;
import com.team5.onlineshopping.checkout.dto.CheckoutItemResponse;
import com.team5.onlineshopping.checkout.dto.CheckoutRequest;
import com.team5.onlineshopping.checkout.dto.CheckoutResponse;
import com.team5.onlineshopping.checkout.dto.CouponApplyRequest;
import com.team5.onlineshopping.checkout.dto.CouponApplyResponse;
import com.team5.onlineshopping.checkout.service.CheckoutService;
import com.team5.onlineshopping.common.enums.OrderStatus;
import com.team5.onlineshopping.common.enums.PaymentMethod;
import com.team5.onlineshopping.common.enums.PaymentStatus;
import com.team5.onlineshopping.common.enums.Role;
import com.team5.onlineshopping.security.CustomUserDetails;
import com.team5.onlineshopping.security.JwtService;
import com.team5.onlineshopping.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    value = CheckoutController.class,
    excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@org.springframework.context.annotation.Import(com.team5.onlineshopping.config.SecurityConfig.class)
@DisplayName("CheckoutController Slice Tests")
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CheckoutService checkoutService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private CustomUserDetails customerDetails;
    private AddressDto addressDto;

    @BeforeEach
    void setUp() {
        User customer = new User("customer@test.com", "hash", "Customer User", "1234567890", Role.CUSTOMER);
        customer.setId(10L);
        customerDetails = new CustomUserDetails(customer);

        addressDto = new AddressDto();
        addressDto.setRecipientName("Customer User");
        addressDto.setPhoneNumber("1234567890");
        addressDto.setStreetLine1("123 Market St");
        addressDto.setCity("Bengaluru");
        addressDto.setState("Karnataka");
        addressDto.setPostalCode("560001");
        addressDto.setCountry("India");
    }

    private void authenticateAsCustomer() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(customerDetails, null, customerDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void authenticateAsSeller() {
        User seller = new User("seller@test.com", "hash", "Seller User", "0987654321", Role.SELLER);
        seller.setId(20L);
        CustomUserDetails sellerDetails = new CustomUserDetails(seller);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(sellerDetails, null, sellerDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("POST /api/checkout should return 401 when unauthenticated")
    void testCheckout_Unauthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        CheckoutRequest request = new CheckoutRequest(addressDto, PaymentMethod.UPI, null);

        mockMvc.perform(post("/api/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/checkout should return 403 when authenticated as SELLER")
    void testCheckout_ForbiddenForSeller() throws Exception {
        authenticateAsSeller();

        CheckoutRequest request = new CheckoutRequest(addressDto, PaymentMethod.UPI, null);

        mockMvc.perform(post("/api/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/checkout should reject request with invalid postal code")
    void testCheckout_InvalidAddress() throws Exception {
        authenticateAsCustomer();

        addressDto.setPostalCode("ABC"); // Invalid pattern
        CheckoutRequest request = new CheckoutRequest(addressDto, PaymentMethod.UPI, null);

        mockMvc.perform(post("/api/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/checkout should create order and return 201 Created")
    void testCheckout_Success() throws Exception {
        authenticateAsCustomer();

        CheckoutRequest request = new CheckoutRequest(addressDto, PaymentMethod.UPI, "SAVE10");

        CheckoutResponse mockResponse = new CheckoutResponse();
        mockResponse.setOrderId(101L);
        mockResponse.setOrderNumber("ORD-12345");
        mockResponse.setCheckoutReference("CHK-555");
        mockResponse.setReservationCode("RES-777");
        mockResponse.setOrderStatus(OrderStatus.CREATED);
        mockResponse.setPaymentStatus(PaymentStatus.PENDING);
        mockResponse.setPaymentMethod(PaymentMethod.UPI);
        mockResponse.setSubtotalAmount(new BigDecimal("1000.00"));
        mockResponse.setDiscountAmount(new BigDecimal("100.00"));
        mockResponse.setCouponCode("SAVE10");
        mockResponse.setTotalAmount(new BigDecimal("900.00"));
        mockResponse.setDeliveryAddress(addressDto);
        mockResponse.setCreatedAt(Instant.now());

        CheckoutItemResponse item = new CheckoutItemResponse();
        item.setOrderItemId(1L);
        item.setProductId(50L);
        item.setProductName("Laptop");
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("1000.00"));
        item.setGrossAmount(new BigDecimal("1000.00"));
        item.setAllocatedDiscount(new BigDecimal("100.00"));
        item.setNetPaidAmount(new BigDecimal("900.00"));
        mockResponse.setItems(List.of(item));

        when(checkoutService.processCheckout(eq(10L), any(CheckoutRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(101))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-12345"))
                .andExpect(jsonPath("$.data.totalAmount").value(900.00))
                .andExpect(jsonPath("$.data.items[0].netPaidAmount").value(900.00));
    }

    @Test
    @DisplayName("POST /api/coupons/apply should return 200 with calculated discount")
    void testApplyCoupon_Success() throws Exception {
        authenticateAsCustomer();

        CouponApplyRequest request = new CouponApplyRequest("FESTIVE25");
        CouponApplyResponse response = new CouponApplyResponse(
                "FESTIVE25",
                "PERCENTAGE",
                new BigDecimal("125.00"),
                new BigDecimal("500.00"),
                new BigDecimal("375.00")
        );

        when(checkoutService.validateCartCoupon(10L, "FESTIVE25")).thenReturn(response);

        mockMvc.perform(post("/api/coupons/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.couponCode").value("FESTIVE25"))
                .andExpect(jsonPath("$.data.discountAmount").value(125.00))
                .andExpect(jsonPath("$.data.finalPayable").value(375.00));
    }
}
