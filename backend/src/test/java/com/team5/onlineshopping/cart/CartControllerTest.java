package com.team5.onlineshopping.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.onlineshopping.cart.controller.CartController;
import com.team5.onlineshopping.cart.dto.CartItemRequest;
import com.team5.onlineshopping.cart.dto.CartItemResponse;
import com.team5.onlineshopping.cart.dto.CartItemUpdateRequest;
import com.team5.onlineshopping.cart.dto.CartResponse;
import com.team5.onlineshopping.cart.service.CartService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    value = CartController.class,
    excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@org.springframework.context.annotation.Import(com.team5.onlineshopping.config.SecurityConfig.class)
@DisplayName("CartController Slice Tests")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private CustomUserDetails customerDetails;
    private CartResponse mockCartResponse;

    @BeforeEach
    void setUp() {
        User customer = new User("customer@test.com", "hash", "Customer User", "1234567890", Role.CUSTOMER);
        customer.setId(10L);
        customerDetails = new CustomUserDetails(customer);

        CartItemResponse item = new CartItemResponse();
        item.setId(101L);
        item.setProductId(1L);
        item.setProductName("Laptop");
        item.setProductBrand("Brand");
        item.setProductCategory("Electronics");
        item.setUnitPrice(new BigDecimal("999.99"));
        item.setQuantity(1);
        item.setLineTotal(new BigDecimal("999.99"));
        item.setAvailableStock(10);
        item.setInStock(true);

        mockCartResponse = new CartResponse();
        mockCartResponse.setId(1L);
        mockCartResponse.setCustomerId(10L);
        mockCartResponse.setItems(List.of(item));
        mockCartResponse.setTotalItems(1);
        mockCartResponse.setSubtotal(new BigDecimal("999.99"));
        mockCartResponse.setShippingFee(BigDecimal.ZERO);
        mockCartResponse.setTotal(new BigDecimal("999.99"));
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
    @DisplayName("GET /api/cart should return 401 when unauthenticated")
    void testGetCart_Unauthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/cart should return 403 when authenticated as SELLER")
    void testGetCart_ForbiddenForSeller() throws Exception {
        authenticateAsSeller();

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/cart should return 200 with cart for CUSTOMER")
    void testGetCart_Success() throws Exception {
        authenticateAsCustomer();
        when(cartService.getCustomerCart(10L)).thenReturn(mockCartResponse);

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.customerId").value(10))
                .andExpect(jsonPath("$.data.items[0].productName").value("Laptop"))
                .andExpect(jsonPath("$.data.total").value(999.99));
    }

    @Test
    @DisplayName("POST /api/cart/items should add item and return 201 Created")
    void testAddToCart_Success() throws Exception {
        authenticateAsCustomer();
        CartItemRequest request = new CartItemRequest(1L, 1);
        when(cartService.addToCart(eq(10L), any(CartItemRequest.class))).thenReturn(mockCartResponse);

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @DisplayName("PUT /api/cart/items/{itemId} should update quantity and return 200")
    void testUpdateQuantity_Success() throws Exception {
        authenticateAsCustomer();
        CartItemUpdateRequest request = new CartItemUpdateRequest(3);
        when(cartService.updateCartItemQuantity(eq(10L), eq(101L), any(CartItemUpdateRequest.class)))
                .thenReturn(mockCartResponse);

        mockMvc.perform(put("/api/cart/items/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/cart/items/{itemId} should remove item and return 200")
    void testRemoveItem_Success() throws Exception {
        authenticateAsCustomer();
        when(cartService.removeCartItem(10L, 101L)).thenReturn(mockCartResponse);

        mockMvc.perform(delete("/api/cart/items/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/cart should clear cart and return 200")
    void testClearCart_Success() throws Exception {
        authenticateAsCustomer();
        CartResponse emptyCartResponse = new CartResponse();
        emptyCartResponse.setId(1L);
        emptyCartResponse.setCustomerId(10L);
        emptyCartResponse.setItems(new java.util.ArrayList<>());
        emptyCartResponse.setTotalItems(0);
        emptyCartResponse.setSubtotal(BigDecimal.ZERO);
        emptyCartResponse.setShippingFee(BigDecimal.ZERO);
        emptyCartResponse.setTotal(BigDecimal.ZERO);

        when(cartService.clearCart(10L)).thenReturn(emptyCartResponse);

        mockMvc.perform(delete("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }
}
