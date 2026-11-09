package com.team5.onlineshopping.checkout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.onlineshopping.cart.entity.Cart;
import com.team5.onlineshopping.cart.entity.CartItem;
import com.team5.onlineshopping.cart.repository.CartRepository;
import com.team5.onlineshopping.checkout.dto.AddressDto;
import com.team5.onlineshopping.checkout.dto.CheckoutRequest;
import com.team5.onlineshopping.checkout.dto.CheckoutResponse;
import com.team5.onlineshopping.checkout.dto.CouponApplyResponse;
import com.team5.onlineshopping.checkout.service.CheckoutServiceImpl;
import com.team5.onlineshopping.common.enums.DiscountType;
import com.team5.onlineshopping.common.enums.OrderStatus;
import com.team5.onlineshopping.common.enums.PaymentMethod;
import com.team5.onlineshopping.common.enums.PaymentStatus;
import com.team5.onlineshopping.common.enums.ReservationStatus;
import com.team5.onlineshopping.common.enums.Role;
import com.team5.onlineshopping.common.exception.BadRequestException;
import com.team5.onlineshopping.coupon.dto.CouponReservationResult;
import com.team5.onlineshopping.coupon.dto.CouponValidationResult;
import com.team5.onlineshopping.coupon.entity.Coupon;
import com.team5.onlineshopping.coupon.repository.CouponRepository;
import com.team5.onlineshopping.coupon.service.CouponService;
import com.team5.onlineshopping.inventory.dto.StockReservationRequest;
import com.team5.onlineshopping.inventory.dto.StockReservationResult;
import com.team5.onlineshopping.inventory.service.InventoryService;
import com.team5.onlineshopping.order.entity.Order;
import com.team5.onlineshopping.order.entity.OrderItem;
import com.team5.onlineshopping.order.repository.OrderRepository;
import com.team5.onlineshopping.product.entity.Product;
import com.team5.onlineshopping.product.repository.ProductRepository;
import com.team5.onlineshopping.user.entity.User;
import com.team5.onlineshopping.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckoutService Unit Tests")
class CheckoutServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private CouponService couponService;

    private ObjectMapper objectMapper;
    private CheckoutServiceImpl checkoutService;

    private User customer;
    private User seller;
    private Product product1;
    private Product product2;
    private Cart cart;
    private AddressDto deliveryAddress;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        checkoutService = new CheckoutServiceImpl(
                cartRepository,
                productRepository,
                orderRepository,
                userRepository,
                couponRepository,
                inventoryService,
                couponService,
                objectMapper
        );

        customer = new User("customer@test.com", "hash", "Test Customer", "1234567890", Role.CUSTOMER);
        customer.setId(10L);

        seller = new User("seller@test.com", "hash", "Test Seller", "0987654321", Role.SELLER);
        seller.setId(20L);

        product1 = new Product(seller, "Product A", "BrandX", "CatA", "Description A", null, new BigDecimal("300.00"), 10);
        product1.setId(101L);

        product2 = new Product(seller, "Product B", "BrandY", "CatB", "Description B", null, new BigDecimal("200.00"), 5);
        product2.setId(102L);

        cart = new Cart(customer);
        cart.setId(1L);
        cart.setItems(new ArrayList<>());

        deliveryAddress = new AddressDto();
        deliveryAddress.setRecipientName("Test Customer");
        deliveryAddress.setPhoneNumber("1234567890");
        deliveryAddress.setStreetLine1("123 Test St");
        deliveryAddress.setCity("Bengaluru");
        deliveryAddress.setState("Karnataka");
        deliveryAddress.setPostalCode("560001");
        deliveryAddress.setCountry("India");
    }

    @Test
    @DisplayName("Should validate coupon successfully against server cart subtotal")
    void testValidateCartCoupon_Success() {
        CartItem item = new CartItem(cart, product1, 2); // 2 * 300 = 600
        cart.addItem(item);

        when(cartRepository.findByCustomerIdWithItems(10L)).thenReturn(Optional.of(cart));
        when(couponService.validateCoupon("FESTIVE25", 10L, new BigDecimal("600.00")))
                .thenReturn(CouponValidationResult.valid("FESTIVE25", new BigDecimal("150.00")));

        Coupon coupon = new Coupon("FESTIVE25", DiscountType.PERCENTAGE, new BigDecimal("25.00"),
                BigDecimal.ZERO, Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(10, ChronoUnit.DAYS));
        when(couponRepository.findByCode("FESTIVE25")).thenReturn(Optional.of(coupon));

        CouponApplyResponse response = checkoutService.validateCartCoupon(10L, "FESTIVE25");

        assertThat(response).isNotNull();
        assertThat(response.getCouponCode()).isEqualTo("FESTIVE25");
        assertThat(response.getDiscountType()).isEqualTo("PERCENTAGE");
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(response.getVerifiedSubtotal()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(response.getFinalPayable()).isEqualByComparingTo(new BigDecimal("450.00"));
    }

    @Test
    @DisplayName("Should reject checkout when cart is empty")
    void testProcessCheckout_EmptyCart() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerIdWithItems(10L)).thenReturn(Optional.of(cart));

        CheckoutRequest request = new CheckoutRequest(deliveryAddress, PaymentMethod.UPI, null);

        assertThatThrownBy(() -> checkoutService.processCheckout(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cart is empty");
    }

    @Test
    @DisplayName("Should reject checkout when requested quantity exceeds available product stock")
    void testProcessCheckout_InsufficientStock() {
        CartItem item = new CartItem(cart, product1, 15); // product1 stock is 10
        cart.addItem(item);

        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerIdWithItems(10L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));

        CheckoutRequest request = new CheckoutRequest(deliveryAddress, PaymentMethod.UPI, null);

        assertThatThrownBy(() -> checkoutService.processCheckout(10L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("Should process checkout, call inventory and coupon reservations, allocate discounts, and clear cart")
    void testProcessCheckout_SuccessWithCouponAndInventoryReservation() {
        // Item 1: 1 x 300 = 300
        CartItem item1 = new CartItem(cart, product1, 1);
        cart.addItem(item1);

        // Item 2: 1 x 200 = 200
        CartItem item2 = new CartItem(cart, product2, 1);
        cart.addItem(item2);

        // Subtotal = 500
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerIdWithItems(10L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(102L)).thenReturn(Optional.of(product2));

        // Coupon setup: 100 discount on 500 subtotal
        when(couponService.validateCoupon(eq("FLAT100"), eq(10L), any(BigDecimal.class)))
                .thenReturn(CouponValidationResult.valid("FLAT100", new BigDecimal("100.00")));
        when(couponService.reserveCoupon(eq("FLAT100"), eq(10L), any(String.class)))
                .thenReturn(new CouponReservationResult(true, "FLAT100", "CHK-REF", new BigDecimal("100.00"),
                        Instant.now().plus(15, ChronoUnit.MINUTES), "Reserved"));

        // Inventory reservation setup
        when(inventoryService.reserveStock(any(StockReservationRequest.class)))
                .thenReturn(new StockReservationResult("CHK-REF", "RES-12345", ReservationStatus.ACTIVE,
                        List.of(), Instant.now().plus(15, ChronoUnit.MINUTES)));

        // Order save setup
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(99L);
            long idGen = 1000L;
            for (OrderItem oi : o.getItems()) {
                oi.setId(idGen++);
            }
            return o;
        });

        CheckoutRequest request = new CheckoutRequest(deliveryAddress, PaymentMethod.UPI, "FLAT100");
        CheckoutResponse response = checkoutService.processCheckout(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(99L);
        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getReservationCode()).isEqualTo("RES-12345");
        assertThat(response.getSubtotalAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(response.getItems()).hasSize(2);

        // Verify discount allocation invariant:
        // Item 1 gross: 300, discount: 60.00, net: 240.00
        // Item 2 gross: 200, discount: 40.00, net: 160.00
        // Sum(discounts) = 100.00, Sum(net) = 400.00
        BigDecimal totalDiscountAllocated = BigDecimal.ZERO;
        BigDecimal totalNetPaid = BigDecimal.ZERO;
        for (var item : response.getItems()) {
            totalDiscountAllocated = totalDiscountAllocated.add(item.getAllocatedDiscount());
            totalNetPaid = totalNetPaid.add(item.getNetPaidAmount());
        }
        assertThat(totalDiscountAllocated).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(totalNetPaid).isEqualByComparingTo(new BigDecimal("400.00"));

        // Verify cart cleared
        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);
        verify(inventoryService).reserveStock(any(StockReservationRequest.class));
        verify(couponService).reserveCoupon(eq("FLAT100"), eq(10L), any(String.class));
    }

    @Test
    @DisplayName("Should release coupon reservation safely if inventory reservation throws exception")
    void testProcessCheckout_RollbackCouponOnInventoryFailure() {
        CartItem item = new CartItem(cart, product1, 1);
        cart.addItem(item);

        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerIdWithItems(10L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));

        when(couponService.validateCoupon(eq("DISC10"), eq(10L), any(BigDecimal.class)))
                .thenReturn(CouponValidationResult.valid("DISC10", new BigDecimal("10.00")));
        when(couponService.reserveCoupon(eq("DISC10"), eq(10L), any(String.class)))
                .thenReturn(new CouponReservationResult(true, "DISC10", "CHK-REF", new BigDecimal("10.00"),
                        Instant.now().plus(15, ChronoUnit.MINUTES), "Reserved"));

        when(inventoryService.reserveStock(any(StockReservationRequest.class)))
                .thenThrow(new IllegalStateException("Stock reservation failed"));

        CheckoutRequest request = new CheckoutRequest(deliveryAddress, PaymentMethod.UPI, "DISC10");

        assertThatThrownBy(() -> checkoutService.processCheckout(10L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stock reservation failed");

        verify(couponService).releaseCouponReservation(any(String.class));
    }
}
