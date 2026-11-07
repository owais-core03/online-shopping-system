package com.team5.onlineshopping.product;

import com.team5.onlineshopping.common.enums.Role;
import com.team5.onlineshopping.common.exception.BadRequestException;
import com.team5.onlineshopping.common.exception.ResourceNotFoundException;
import com.team5.onlineshopping.product.dto.ProductCreateRequest;
import com.team5.onlineshopping.product.dto.ProductResponse;
import com.team5.onlineshopping.product.dto.ProductReviewRequest;
import com.team5.onlineshopping.product.dto.ProductReviewResponse;
import com.team5.onlineshopping.product.dto.ProductUpdateRequest;
import com.team5.onlineshopping.product.entity.Product;
import com.team5.onlineshopping.product.entity.ProductReview;
import com.team5.onlineshopping.product.repository.ProductRepository;
import com.team5.onlineshopping.product.repository.ProductReviewRepository;
import com.team5.onlineshopping.product.service.ProductService;
import com.team5.onlineshopping.user.entity.User;
import com.team5.onlineshopping.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductReviewRepository productReviewRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProductService productService;

    private User seller;
    private User customer;
    private Product product;

    @BeforeEach
    void setUp() {
        seller = new User("seller@example.com", "hash", "Tech Seller", "+1234567890", Role.SELLER);
        seller.setId(10L);

        customer = new User("customer@example.com", "hash", "Jane Customer", "+1987654321", Role.CUSTOMER);
        customer.setId(20L);

        product = new Product(
                seller,
                "Wireless Noise-Canceling Headphones",
                "Sony",
                "Electronics",
                "Premium wireless over-ear headphones",
                "{\"color\":\"black\"}",
                new BigDecimal("299.99"),
                50
        );
        product.setId(100L);
    }

    @Test
    @DisplayName("getAllActiveProducts returns list of active product responses")
    void getAllActiveProductsSuccess() {
        when(productRepository.findByIsActiveTrue()).thenReturn(List.of(product));

        List<ProductResponse> result = productService.getAllActiveProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Wireless Noise-Canceling Headphones");
        assertThat(result.get(0).getSellerName()).isEqualTo("Tech Seller");
    }

    @Test
    @DisplayName("getProductById returns product when found and active")
    void getProductByIdSuccess() {
        when(productRepository.findByIdAndIsActiveTrue(100L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(100L);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getBrand()).isEqualTo("Sony");
    }

    @Test
    @DisplayName("getProductById throws ResourceNotFoundException when product does not exist")
    void getProductByIdNotFound() {
        when(productRepository.findByIdAndIsActiveTrue(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: 999");
    }

    @Test
    @DisplayName("createProduct succeeds when user is a SELLER")
    void createProductSuccess() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Smart Watch",
                "Apple",
                "Wearables",
                "Series 9",
                "{\"size\":\"45mm\"}",
                new BigDecimal("399.00"),
                30
        );

        when(userRepository.findById(10L)).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        ProductResponse response = productService.createProduct(10L, request);

        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getName()).isEqualTo("Smart Watch");
        assertThat(response.getSellerId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("createProduct throws BadRequestException when user is CUSTOMER")
    void createProductByCustomerFails() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Smart Watch", "Apple", "Wearables", "Series 9", null, new BigDecimal("399.00"), 30
        );

        when(userRepository.findById(20L)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> productService.createProduct(20L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only sellers or admins can create products");
    }

    @Test
    @DisplayName("updateProduct updates fields when user is product seller")
    void updateProductSuccess() {
        ProductUpdateRequest updateRequest = new ProductUpdateRequest(
                "Updated Name", null, null, null, null, new BigDecimal("249.99"), 25, null
        );

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(userRepository.findById(10L)).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.updateProduct(100L, 10L, updateRequest);

        assertThat(response.getName()).isEqualTo("Updated Name");
        assertThat(response.getPrice()).isEqualTo(new BigDecimal("249.99"));
        assertThat(response.getAvailableStock()).isEqualTo(25);
    }

    @Test
    @DisplayName("updateProduct throws BadRequestException when unauthorized seller attempts update")
    void updateProductUnauthorizedFails() {
        User otherSeller = new User("other@example.com", "hash", "Other", null, Role.SELLER);
        otherSeller.setId(99L);

        ProductUpdateRequest updateRequest = new ProductUpdateRequest();

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(userRepository.findById(99L)).thenReturn(Optional.of(otherSeller));

        assertThatThrownBy(() -> productService.updateProduct(100L, 99L, updateRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("You are not authorized to update this product");
    }

    @Test
    @DisplayName("addReview saves review and updates average rating and review count")
    void addReviewSuccess() {
        ProductReviewRequest reviewRequest = new ProductReviewRequest(5, "Excellent sound quality!");

        when(productRepository.findByIdAndIsActiveTrue(100L)).thenReturn(Optional.of(product));
        when(userRepository.findById(20L)).thenReturn(Optional.of(customer));
        when(productReviewRepository.findByProductIdAndCustomerId(100L, 20L)).thenReturn(Optional.empty());

        ProductReview savedReview = new ProductReview(product, customer, 5, "Excellent sound quality!");
        savedReview.setId(500L);
        when(productReviewRepository.save(any(ProductReview.class))).thenReturn(savedReview);
        when(productReviewRepository.findByProductIdAndIsApprovedTrue(100L)).thenReturn(List.of(savedReview));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductReviewResponse response = productService.addReview(100L, 20L, reviewRequest);

        assertThat(response.getId()).isEqualTo(500L);
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(product.getReviewCount()).isEqualTo(1);
        assertThat(product.getAverageRating()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    @DisplayName("addReview throws BadRequestException if customer already reviewed product")
    void addReviewDuplicateFails() {
        ProductReviewRequest reviewRequest = new ProductReviewRequest(4, "Second review attempt");

        when(productRepository.findByIdAndIsActiveTrue(100L)).thenReturn(Optional.of(product));
        when(userRepository.findById(20L)).thenReturn(Optional.of(customer));
        when(productReviewRepository.findByProductIdAndCustomerId(100L, 20L))
                .thenReturn(Optional.of(new ProductReview()));

        assertThatThrownBy(() -> productService.addReview(100L, 20L, reviewRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("You have already reviewed this product");
    }
}
