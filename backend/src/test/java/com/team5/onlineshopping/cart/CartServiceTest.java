package com.team5.onlineshopping.cart;

import com.team5.onlineshopping.cart.dto.CartItemRequest;
import com.team5.onlineshopping.cart.dto.CartItemResponse;
import com.team5.onlineshopping.cart.dto.CartItemUpdateRequest;
import com.team5.onlineshopping.cart.dto.CartResponse;
import com.team5.onlineshopping.cart.entity.Cart;
import com.team5.onlineshopping.cart.entity.CartItem;
import com.team5.onlineshopping.cart.repository.CartItemRepository;
import com.team5.onlineshopping.cart.repository.CartRepository;
import com.team5.onlineshopping.cart.service.CartServiceImpl;
import com.team5.onlineshopping.common.enums.Role;
import com.team5.onlineshopping.common.exception.BadRequestException;
import com.team5.onlineshopping.common.exception.ResourceNotFoundException;
import com.team5.onlineshopping.product.entity.Product;
import com.team5.onlineshopping.product.repository.ProductRepository;
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
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Unit Tests")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private User customer;
    private User seller;
    private Product product1;
    private Product product2;
    private Cart cart;

    @BeforeEach
    void setUp() {
        customer = new User("customer@test.com", "hash", "Test Customer", "1234567890", Role.CUSTOMER);
        customer.setId(10L);

        seller = new User("seller@test.com", "hash", "Test Seller", "0987654321", Role.SELLER);
        seller.setId(20L);

        product1 = new Product(seller, "Smartphone", "TechBrand", "Electronics", "Description", null, new BigDecimal("500.00"), 10);
        product1.setId(101L);

        product2 = new Product(seller, "Headphones", "AudioBrand", "Electronics", "Description", null, new BigDecimal("100.00"), 5);
        product2.setId(102L);

        cart = new Cart(customer);
        cart.setId(1L);
        cart.setItems(new ArrayList<>());
    }

    @Test
    @DisplayName("Should return empty cart for customer with no items")
    void testGetCustomerCart_Empty() {
        when(cartRepository.findByCustomerIdWithItems(10L)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCustomerCart(10L);

        assertThat(response).isNotNull();
        assertThat(response.getCustomerId()).isEqualTo(10L);
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalItems()).isZero();
    }

    @Test
    @DisplayName("Should add new product to cart and calculate correct totals")
    void testAddToCart_NewProduct() {
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
        when(cartRepository.findByCustomerIdWithItems(10L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem item = invocation.getArgument(0);
            item.setId(501L);
            return item;
        });

        CartItemRequest request = new CartItemRequest(101L, 2);
        CartResponse response = cartService.addToCart(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        CartItemResponse item = response.getItems().getFirst();
        assertThat(item.getProductId()).isEqualTo(101L);
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getUnitPrice()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(item.getLineTotal()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(response.getSubtotal()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(response.getTotal()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(response.getTotalItems()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should increase quantity when adding existing product in cart")
    void testAddToCart_ExistingProductIncrementsQuantity() {
        CartItem existingItem = new CartItem(cart, product1, 2);
        existingItem.setId(501L);
        cart.addItem(existingItem);

        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
        when(cartRepository.findByCustomerIdWithItems(10L)).thenReturn(Optional.of(cart));

        CartItemRequest request = new CartItemRequest(101L, 3);
        CartResponse response = cartService.addToCart(10L, request);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().getFirst().getQuantity()).isEqualTo(5);
        assertThat(response.getSubtotal()).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(response.getTotalItems()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should reject adding product when requested quantity exceeds available stock")
    void testAddToCart_InsufficientStock() {
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
        when(cartRepository.findByCustomerIdWithItems(10L)).thenReturn(Optional.of(cart));

        CartItemRequest request = new CartItemRequest(101L, 15); // product1 availableStock is 10

        assertThatThrownBy(() -> cartService.addToCart(10L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("Should reject adding inactive product")
    void testAddToCart_InactiveProduct() {
        product1.setActive(false);
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));

        CartItemRequest request = new CartItemRequest(101L, 1);

        assertThatThrownBy(() -> cartService.addToCart(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("Should update cart item quantity successfully")
    void testUpdateCartItemQuantity_Success() {
        CartItem item = new CartItem(cart, product1, 2);
        item.setId(501L);
        cart.addItem(item);

        when(cartRepository.findByCustomerIdWithItems(10L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartCustomerId(501L, 10L)).thenReturn(Optional.of(item));

        CartItemUpdateRequest updateRequest = new CartItemUpdateRequest(4);
        CartResponse response = cartService.updateCartItemQuantity(10L, 501L, updateRequest);

        assertThat(response.getItems().getFirst().getQuantity()).isEqualTo(4);
        assertThat(response.getSubtotal()).isEqualByComparingTo(new BigDecimal("2000.00"));
    }

    @Test
    @DisplayName("Should reject quantity update exceeding available stock")
    void testUpdateCartItemQuantity_ExceedsStock() {
        CartItem item = new CartItem(cart, product1, 2);
        item.setId(501L);
        cart.addItem(item);

        when(cartRepository.findByCustomerIdWithItems(10L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartCustomerId(501L, 10L)).thenReturn(Optional.of(item));

        CartItemUpdateRequest updateRequest = new CartItemUpdateRequest(20);

        assertThatThrownBy(() -> cartService.updateCartItemQuantity(10L, 501L, updateRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("Should remove item from cart and recalculate subtotal")
    void testRemoveCartItem_Success() {
        CartItem item1 = new CartItem(cart, product1, 2);
        item1.setId(501L);
        cart.addItem(item1);

        CartItem item2 = new CartItem(cart, product2, 1);
        item2.setId(502L);
        cart.addItem(item2);

        when(cartRepository.findByCustomerIdWithItems(10L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartCustomerId(501L, 10L)).thenReturn(Optional.of(item1));

        CartResponse response = cartService.removeCartItem(10L, 501L);

        verify(cartItemRepository).delete(item1);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().getFirst().getProductId()).isEqualTo(102L);
        assertThat(response.getSubtotal()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Should clear all items from cart")
    void testClearCart_Success() {
        CartItem item1 = new CartItem(cart, product1, 2);
        cart.addItem(item1);

        when(cartRepository.findByCustomerIdWithItems(10L)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.clearCart(10L);

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalItems()).isZero();
    }
}
