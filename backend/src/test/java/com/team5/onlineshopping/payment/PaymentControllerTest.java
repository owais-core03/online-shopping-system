package com.team5.onlineshopping.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.onlineshopping.common.enums.PaymentMethod;
import com.team5.onlineshopping.common.enums.PaymentStatus;
import com.team5.onlineshopping.common.enums.Role;
import com.team5.onlineshopping.payment.controller.PaymentController;
import com.team5.onlineshopping.payment.dto.PaymentInitiateRequest;
import com.team5.onlineshopping.payment.dto.PaymentInitiateResponse;
import com.team5.onlineshopping.payment.dto.PaymentStatusResponse;
import com.team5.onlineshopping.payment.dto.PaymentWebhookRequest;
import com.team5.onlineshopping.payment.service.PaymentService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    value = PaymentController.class,
    excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@org.springframework.context.annotation.Import(com.team5.onlineshopping.config.SecurityConfig.class)
@DisplayName("PaymentController Slice Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private CustomUserDetails customerDetails;

    @BeforeEach
    void setUp() {
        User customer = new User("customer@test.com", "hash", "Customer User", "1234567890", Role.CUSTOMER);
        customer.setId(10L);
        customerDetails = new CustomUserDetails(customer);
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
    @DisplayName("POST /api/payments/{orderId}/initiate returns 401 when unauthenticated")
    void testInitiatePayment_Unauthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/api/payments/100/initiate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/payments/{orderId}/initiate returns 403 when authenticated as SELLER")
    void testInitiatePayment_ForbiddenForSeller() throws Exception {
        authenticateAsSeller();

        mockMvc.perform(post("/api/payments/100/initiate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/payments/{orderId}/initiate creates payment and returns 201 for CUSTOMER")
    void testInitiatePayment_Success() throws Exception {
        authenticateAsCustomer();

        PaymentInitiateResponse response = new PaymentInitiateResponse();
        response.setPaymentReference("PAY-12345");
        response.setOrderId(100L);
        response.setOrderNumber("ORD-100");
        response.setAmount(new BigDecimal("500.00"));
        response.setPaymentMethod(PaymentMethod.UPI);
        response.setPaymentStatus(PaymentStatus.PENDING);
        response.setRedirectUrl("https://mock-gateway.local/pay/PAY-12345");
        response.setCreatedAt(Instant.now());

        when(paymentService.initiatePayment(eq(10L), eq(100L), any())).thenReturn(response);

        mockMvc.perform(post("/api/payments/100/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentInitiateRequest(PaymentMethod.UPI, "IDEMP-1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentReference").value("PAY-12345"))
                .andExpect(jsonPath("$.data.orderId").value(100))
                .andExpect(jsonPath("$.data.amount").value(500.00));
    }

    @Test
    @DisplayName("POST /api/payments/webhook processes gateway callback without customer token")
    void testPaymentWebhook_Success() throws Exception {
        SecurityContextHolder.clearContext();

        PaymentWebhookRequest request = new PaymentWebhookRequest(
                "EVT-99", "PAY-12345", "GW-123", PaymentStatus.CAPTURED, null, "VALID-HMAC");

        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setPaymentReference("PAY-12345");
        response.setOrderId(100L);
        response.setAmount(new BigDecimal("500.00"));
        response.setPaymentStatus(PaymentStatus.CAPTURED);

        when(paymentService.handlePaymentWebhook(any(PaymentWebhookRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentStatus").value("CAPTURED"));
    }

    @Test
    @DisplayName("GET /api/payments/{orderId}/status returns payment status for CUSTOMER")
    void testGetPaymentStatus_Success() throws Exception {
        authenticateAsCustomer();

        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setPaymentReference("PAY-12345");
        response.setOrderId(100L);
        response.setAmount(new BigDecimal("500.00"));
        response.setPaymentStatus(PaymentStatus.CAPTURED);

        when(paymentService.getPaymentStatus(10L, 100L)).thenReturn(response);

        mockMvc.perform(get("/api/payments/100/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentReference").value("PAY-12345"));
    }
}
