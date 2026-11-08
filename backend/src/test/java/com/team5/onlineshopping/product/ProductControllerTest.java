package com.team5.onlineshopping.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.onlineshopping.common.enums.Role;
import com.team5.onlineshopping.product.controller.ProductController;
import com.team5.onlineshopping.product.dto.ProductCreateRequest;
import com.team5.onlineshopping.product.dto.ProductResponse;
import com.team5.onlineshopping.product.dto.ProductReviewRequest;
import com.team5.onlineshopping.product.dto.ProductReviewResponse;
import com.team5.onlineshopping.product.service.ProductService;
import com.team5.onlineshopping.security.CustomUserDetails;
import com.team5.onlineshopping.security.JwtService;
import com.team5.onlineshopping.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    value = ProductController.class,
    excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@org.springframework.context.annotation.Import(com.team5.onlineshopping.config.SecurityConfig.class)
@DisplayName("ProductController Slice Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/products returns 200 OK and list of products")
    void getProductsPublicAccessReturnsList() throws Exception {
        ProductResponse product = new ProductResponse();
        product.setId(1L);
        product.setName("Gaming Mouse");
        product.setPrice(new BigDecimal("49.99"));
        product.setActive(true);

        when(productService.searchProducts(null, null, null)).thenReturn(List.of(product));

        mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].name").value("Gaming Mouse"));
    }

    @Test
    @DisplayName("GET /api/products/{id} returns 200 OK and product details")
    void getProductByIdPublicAccessReturnsProduct() throws Exception {
        ProductResponse product = new ProductResponse();
        product.setId(5L);
        product.setName("Mechanical Keyboard");
        product.setPrice(new BigDecimal("99.99"));

        when(productService.getProductById(5L)).thenReturn(product);

        mockMvc.perform(get("/api/products/5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(5L))
                .andExpect(jsonPath("$.data.name").value("Mechanical Keyboard"));
    }

    @Test
    @DisplayName("POST /api/products by SELLER creates product and returns 201 Created")
    void createProductBySellerReturnsCreated() throws Exception {
        User sellerUser = new User("seller@shop.com", "hash", "Shop Seller", "+123", Role.SELLER);
        sellerUser.setId(15L);
        CustomUserDetails sellerPrincipal = new CustomUserDetails(sellerUser);

        ProductCreateRequest request = new ProductCreateRequest(
                "4K Monitor", "Dell", "Displays", "Ultra HD", "{\"refreshRate\":\"144Hz\"}",
                new BigDecimal("499.00"), 20
        );

        ProductResponse response = new ProductResponse();
        response.setId(200L);
        response.setName("4K Monitor");
        response.setSellerId(15L);
        response.setPrice(new BigDecimal("499.00"));

        when(productService.createProduct(eq(15L), any(ProductCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .with(user(sellerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product created successfully"))
                .andExpect(jsonPath("$.data.id").value(200L))
                .andExpect(jsonPath("$.data.name").value("4K Monitor"));
    }

    @Test
    @DisplayName("POST /api/products/{id}/reviews by CUSTOMER returns 201 Created")
    void addReviewByCustomerReturnsCreated() throws Exception {
        User customerUser = new User("cust@shop.com", "hash", "Bob Cust", "+123", Role.CUSTOMER);
        customerUser.setId(30L);
        CustomUserDetails customerPrincipal = new CustomUserDetails(customerUser);

        ProductReviewRequest reviewRequest = new ProductReviewRequest(5, "Works perfectly");

        ProductReviewResponse reviewResponse = new ProductReviewResponse();
        reviewResponse.setId(300L);
        reviewResponse.setProductId(5L);
        reviewResponse.setCustomerId(30L);
        reviewResponse.setRating(5);
        reviewResponse.setComment("Works perfectly");

        when(productService.addReview(eq(5L), eq(30L), any(ProductReviewRequest.class))).thenReturn(reviewResponse);

        mockMvc.perform(post("/api/products/5/reviews")
                        .with(csrf())
                        .with(user(customerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(300L))
                .andExpect(jsonPath("$.data.rating").value(5));
    }

    @Test
    @DisplayName("GET /api/products with pagination and sorting returns 200 OK")
    void getProductsWithPaginationAndSorting() throws Exception {
        ProductResponse product = new ProductResponse();
        product.setId(10L);
        product.setName("Wireless Keyboard");
        product.setPrice(new BigDecimal("79.99"));

        org.springframework.data.domain.Page<ProductResponse> page =
                new org.springframework.data.domain.PageImpl<>(List.of(product));

        when(productService.searchProducts(eq("keyboard"), eq("Accessories"), eq("Logitech"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/products")
                        .param("query", "keyboard")
                        .param("category", "Accessories")
                        .param("brand", "Logitech")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "price")
                        .param("sortDirection", "desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10L))
                .andExpect(jsonPath("$.data[0].name").value("Wireless Keyboard"));
    }

    @Test
    @DisplayName("POST /api/products with invalid body returns 400 Bad Request")
    void createProductInvalidBodyReturnsBadRequest() throws Exception {
        User sellerUser = new User("seller@shop.com", "hash", "Shop Seller", "+123", Role.SELLER);
        sellerUser.setId(15L);
        CustomUserDetails sellerPrincipal = new CustomUserDetails(sellerUser);

        // Blank name and negative price
        ProductCreateRequest invalidRequest = new ProductCreateRequest(
                "", "", "", "desc", null, new BigDecimal("-10.00"), -5
        );

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .with(user(sellerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/products without authentication returns 403 or 401")
    void createProductUnauthenticatedReturnsUnauthorized() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "Monitor", "LG", "Displays", "desc", null, new BigDecimal("200.00"), 5
        );

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/products/seller/my-products requires SELLER or ADMIN role")
    void getMyProductsRequiresSellerRole() throws Exception {
        User customerUser = new User("cust@shop.com", "hash", "Customer", "+123", Role.CUSTOMER);
        customerUser.setId(30L);
        CustomUserDetails customerPrincipal = new CustomUserDetails(customerUser);

        mockMvc.perform(get("/api/products/seller/my-products")
                        .with(user(customerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/products/{id}/reviews with invalid rating returns 400 Bad Request")
    void addReviewInvalidRatingReturnsBadRequest() throws Exception {
        User customerUser = new User("cust@shop.com", "hash", "Bob Cust", "+123", Role.CUSTOMER);
        customerUser.setId(30L);
        CustomUserDetails customerPrincipal = new CustomUserDetails(customerUser);

        // Rating > 5
        ProductReviewRequest invalidReview = new ProductReviewRequest(6, "Invalid rating");

        mockMvc.perform(post("/api/products/5/reviews")
                        .with(csrf())
                        .with(user(customerPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReview)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
