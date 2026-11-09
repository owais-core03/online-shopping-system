package com.team5.onlineshopping.payment;

import com.team5.onlineshopping.audit.entity.StatusHistory;
import com.team5.onlineshopping.audit.repository.StatusHistoryRepository;
import com.team5.onlineshopping.common.enums.OrderItemStatus;
import com.team5.onlineshopping.common.enums.OrderStatus;
import com.team5.onlineshopping.common.enums.PaymentMethod;
import com.team5.onlineshopping.common.enums.PaymentStatus;
import com.team5.onlineshopping.common.enums.ReservationStatus;
import com.team5.onlineshopping.common.enums.Role;
import com.team5.onlineshopping.common.exception.BadRequestException;
import com.team5.onlineshopping.coupon.service.CouponService;
import com.team5.onlineshopping.inventory.entity.StockReservation;
import com.team5.onlineshopping.inventory.repository.StockReservationRepository;
import com.team5.onlineshopping.inventory.service.InventoryService;
import com.team5.onlineshopping.order.entity.Order;
import com.team5.onlineshopping.order.entity.OrderItem;
import com.team5.onlineshopping.order.repository.OrderRepository;
import com.team5.onlineshopping.order.service.OrderStatusService;
import com.team5.onlineshopping.payment.dto.PaymentInitiateRequest;
import com.team5.onlineshopping.payment.dto.PaymentInitiateResponse;
import com.team5.onlineshopping.payment.dto.PaymentStatusResponse;
import com.team5.onlineshopping.payment.dto.PaymentWebhookRequest;
import com.team5.onlineshopping.payment.entity.PaymentTransaction;
import com.team5.onlineshopping.payment.repository.PaymentTransactionRepository;
import com.team5.onlineshopping.payment.service.PaymentGateway;
import com.team5.onlineshopping.payment.service.PaymentServiceImpl;
import com.team5.onlineshopping.product.entity.Product;
import com.team5.onlineshopping.user.entity.User;
import com.team5.onlineshopping.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @Mock
    private StatusHistoryRepository statusHistoryRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private CouponService couponService;

    @Mock
    private OrderStatusService orderStatusService;

    private PaymentServiceImpl paymentService;

    private User customer;
    private User seller;
    private Order order;
    private Product product;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                orderRepository,
                userRepository,
                paymentTransactionRepository,
                stockReservationRepository,
                statusHistoryRepository,
                paymentGateway,
                inventoryService,
                couponService,
                orderStatusService
        );

        customer = new User("customer@test.com", "hash", "Customer User", "1234567890", Role.CUSTOMER);
        customer.setId(10L);

        seller = new User("seller@test.com", "hash", "Seller User", "0987654321", Role.SELLER);
        seller.setId(20L);

        order = new Order("ORD-123", customer, new BigDecimal("500.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("500.00"), PaymentMethod.UPI, "{}");
        order.setId(100L);
        order.setOrderStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING);

        product = new Product(seller, "Phone", "Brand", "Cat", "Desc", null, new BigDecimal("500.00"), 10);
        product.setId(1L);

        orderItem = new OrderItem(order, product, seller, 1, new BigDecimal("500.00"), BigDecimal.ZERO);
        orderItem.setId(1001L);
        orderItem.setItemStatus(OrderItemStatus.PENDING_SELLER_ACCEPTANCE);
        order.addItem(orderItem);
    }

    @Test
    @DisplayName("Should initiate payment successfully for valid order")
    void testInitiatePayment_Success() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(stockReservationRepository.findByCustomerId(10L)).thenReturn(List.of());

        PaymentGateway.GatewayInitiateResult gwResult = new PaymentGateway.GatewayInitiateResult(
                "PAY-REF-1", "MOCK-GW-1", PaymentStatus.PENDING, "https://mock.pay/PAY-REF-1");
        when(paymentGateway.initiatePayment(eq(100L), eq("ORD-123"), any(), any(), any())).thenReturn(gwResult);

        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentInitiateRequest request = new PaymentInitiateRequest(PaymentMethod.UPI, "IDEMP-123");
        PaymentInitiateResponse response = paymentService.initiatePayment(10L, 100L, request);

        assertThat(response).isNotNull();
        assertThat(response.getPaymentReference()).isEqualTo("PAY-REF-1");
        assertThat(response.getOrderId()).isEqualTo(100L);
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentTransactionRepository).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("Should prevent duplicate payment initiation from creating duplicate transactions")
    void testInitiatePayment_IdempotentDuplicate() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        PaymentTransaction existingTx = new PaymentTransaction("PAY-EXISTING", order, customer,
                new BigDecimal("500.00"), PaymentMethod.UPI, "IDEMP-REPEAT");
        when(paymentTransactionRepository.findByIdempotencyKey("IDEMP-REPEAT")).thenReturn(Optional.of(existingTx));

        PaymentInitiateRequest request = new PaymentInitiateRequest(PaymentMethod.UPI, "IDEMP-REPEAT");
        PaymentInitiateResponse response = paymentService.initiatePayment(10L, 100L, request);

        assertThat(response.getPaymentReference()).isEqualTo("PAY-EXISTING");
        verify(paymentGateway, never()).initiatePayment(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should reject payment initiation if order belongs to another customer")
    void testInitiatePayment_UnauthorizedCustomer() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        PaymentInitiateRequest request = new PaymentInitiateRequest(PaymentMethod.UPI, "IDEMP-XYZ");

        assertThatThrownBy(() -> paymentService.initiatePayment(999L, 100L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    @DisplayName("Should reject payment initiation if stock reservation has expired")
    void testInitiatePayment_ExpiredStockReservation() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        StockReservation expiredRes = new StockReservation("RES-CODE", customer, "CHK-123");
        expiredRes.setStatus(ReservationStatus.ACTIVE);
        expiredRes.setExpiresAt(Instant.now().minus(5, ChronoUnit.MINUTES));

        when(stockReservationRepository.findByCustomerId(10L)).thenReturn(List.of(expiredRes));

        PaymentInitiateRequest request = new PaymentInitiateRequest(PaymentMethod.UPI, "IDEMP-EXP");

        assertThatThrownBy(() -> paymentService.initiatePayment(10L, 100L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reservation has expired");

        assertThat(expiredRes.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
    }

    @Test
    @DisplayName("Should process successful webhook: CAPTURED, confirms inventory/coupon, advances order to PROCESSING")
    void testHandleWebhook_SuccessfulPayment() {
        PaymentTransaction transaction = new PaymentTransaction("PAY-REF-1", order, customer,
                new BigDecimal("500.00"), PaymentMethod.UPI, "IDEMP-1");
        transaction.setPaymentStatus(PaymentStatus.PENDING);
        transaction.setCheckoutReference("CHK-ORIGINAL-999");
        order.setCouponCode("DISCOUNT20");

        when(paymentTransactionRepository.findByPaymentReference("PAY-REF-1")).thenReturn(Optional.of(transaction));
        when(paymentGateway.verifyPayment("PAY-REF-1", "VALID-SIGNATURE"))
                .thenReturn(new PaymentGateway.GatewayVerifyResult(true, PaymentStatus.CAPTURED, "GW-REF-99", null));

        PaymentWebhookRequest webhookRequest = new PaymentWebhookRequest(
                "EVT-1", "PAY-REF-1", "GW-REF-99", PaymentStatus.CAPTURED, null, "VALID-SIGNATURE");

        PaymentStatusResponse response = paymentService.handlePaymentWebhook(webhookRequest);

        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(orderItem.getItemStatus()).isEqualTo(OrderItemStatus.PROCESSING);

        // Verify exact checkout reference reached inventoryService and couponService
        verify(inventoryService).confirmReservation("CHK-ORIGINAL-999");
        verify(couponService).confirmCouponUsage("CHK-ORIGINAL-999");
        verify(statusHistoryRepository).save(any(StatusHistory.class));
    }

    @Test
    @DisplayName("Should process failed webhook: FAILED, releases inventory/coupon using exact checkoutReference")
    void testHandleWebhook_FailedPayment() {
        PaymentTransaction transaction = new PaymentTransaction("PAY-REF-2", order, customer,
                new BigDecimal("500.00"), PaymentMethod.UPI, "IDEMP-2");
        transaction.setPaymentStatus(PaymentStatus.PENDING);
        transaction.setCheckoutReference("CHK-ORIGINAL-888");
        order.setCouponCode("DISCOUNT20");

        when(paymentTransactionRepository.findByPaymentReference("PAY-REF-2")).thenReturn(Optional.of(transaction));
        when(paymentGateway.verifyPayment("PAY-REF-2", "SIGNATURE-FAIL"))
                .thenReturn(new PaymentGateway.GatewayVerifyResult(true, PaymentStatus.FAILED, "GW-REF-FAIL", "Insufficient funds"));

        PaymentWebhookRequest webhookRequest = new PaymentWebhookRequest(
                "EVT-2", "PAY-REF-2", "GW-REF-FAIL", PaymentStatus.FAILED, "Insufficient funds", "SIGNATURE-FAIL");

        PaymentStatusResponse response = paymentService.handlePaymentWebhook(webhookRequest);

        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        // Order remains created / not advanced to PROCESSING
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CREATED);

        // Verify reservations released using exact checkout reference
        verify(inventoryService).releaseReservation("CHK-ORIGINAL-888");
        verify(couponService).releaseCouponReservation("CHK-ORIGINAL-888");
        verify(statusHistoryRepository).save(any(StatusHistory.class));
    }

    @Test
    @DisplayName("Should rollback and not mark order or transaction CAPTURED if inventory confirmation throws exception")
    void testHandleWebhook_FailsafeIfReservationConfirmationFails() {
        PaymentTransaction transaction = new PaymentTransaction("PAY-REF-ERR", order, customer,
                new BigDecimal("500.00"), PaymentMethod.UPI, "IDEMP-ERR");
        transaction.setPaymentStatus(PaymentStatus.PENDING);
        transaction.setCheckoutReference("CHK-ERR-111");

        when(paymentTransactionRepository.findByPaymentReference("PAY-REF-ERR")).thenReturn(Optional.of(transaction));
        when(paymentGateway.verifyPayment("PAY-REF-ERR", "VALID-SIG"))
                .thenReturn(new PaymentGateway.GatewayVerifyResult(true, PaymentStatus.CAPTURED, "GW-ERR", null));

        org.mockito.Mockito.doThrow(new IllegalStateException("Stock reservation confirmation failed in external service"))
                .when(inventoryService).confirmReservation("CHK-ERR-111");

        PaymentWebhookRequest webhookRequest = new PaymentWebhookRequest(
                "EVT-ERR", "PAY-REF-ERR", "GW-ERR", PaymentStatus.CAPTURED, null, "VALID-SIG");

        assertThatThrownBy(() -> paymentService.handlePaymentWebhook(webhookRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stock reservation confirmation failed");

        // Transaction and order statuses should not be updated to CAPTURED or PROCESSING
        assertThat(transaction.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CREATED);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should be idempotent when duplicate webhook arrives for already CAPTURED payment")
    void testHandleWebhook_IdempotentDuplicate() {
        PaymentTransaction transaction = new PaymentTransaction("PAY-REF-3", order, customer,
                new BigDecimal("500.00"), PaymentMethod.UPI, "IDEMP-3");
        transaction.setPaymentStatus(PaymentStatus.CAPTURED);

        when(paymentTransactionRepository.findByPaymentReference("PAY-REF-3")).thenReturn(Optional.of(transaction));

        PaymentWebhookRequest webhookRequest = new PaymentWebhookRequest(
                "EVT-DUP", "PAY-REF-3", "GW-REF-3", PaymentStatus.CAPTURED, null, "SIG");

        PaymentStatusResponse response = paymentService.handlePaymentWebhook(webhookRequest);

        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.CAPTURED);
        verify(paymentGateway, never()).verifyPayment(anyString(), anyString());
        verify(inventoryService, never()).confirmReservation(anyString());
    }

    @Test
    @DisplayName("Should reject webhook when gateway signature verification fails")
    void testHandleWebhook_InvalidSignature() {
        PaymentTransaction transaction = new PaymentTransaction("PAY-REF-4", order, customer,
                new BigDecimal("500.00"), PaymentMethod.UPI, "IDEMP-4");
        transaction.setPaymentStatus(PaymentStatus.PENDING);

        when(paymentTransactionRepository.findByPaymentReference("PAY-REF-4")).thenReturn(Optional.of(transaction));
        when(paymentGateway.verifyPayment("PAY-REF-4", "BAD-SIG"))
                .thenReturn(new PaymentGateway.GatewayVerifyResult(false, PaymentStatus.FAILED, null, "Invalid signature"));

        PaymentWebhookRequest webhookRequest = new PaymentWebhookRequest(
                "EVT-4", "PAY-REF-4", null, PaymentStatus.CAPTURED, null, "BAD-SIG");

        assertThatThrownBy(() -> paymentService.handlePaymentWebhook(webhookRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("verification failed");
    }

    @Test
    @DisplayName("Customer can retrieve own order payment status, rejects other customers")
    void testGetPaymentStatus_Ownership() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        PaymentTransaction transaction = new PaymentTransaction("PAY-REF-5", order, customer,
                new BigDecimal("500.00"), PaymentMethod.UPI, "IDEMP-5");
        when(paymentTransactionRepository.findTopByOrderIdOrderByCreatedAtDesc(100L)).thenReturn(Optional.of(transaction));

        PaymentStatusResponse response = paymentService.getPaymentStatus(10L, 100L);
        assertThat(response.getPaymentReference()).isEqualTo("PAY-REF-5");

        assertThatThrownBy(() -> paymentService.getPaymentStatus(999L, 100L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Should reject payment initiation if reservation customer ownership does not match order customer")
    void testInitiatePayment_MismatchedReservationOwnership() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        User otherUser = new User("other@test.com", "hash", "Other User", "0000000000", Role.CUSTOMER);
        otherUser.setId(99L);

        StockReservation forgedRes = new StockReservation("FORGED-RES", otherUser, "FORGED-CHK");
        forgedRes.setStatus(ReservationStatus.ACTIVE);
        forgedRes.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));

        when(stockReservationRepository.findByCustomerId(10L)).thenReturn(List.of(forgedRes));

        PaymentInitiateRequest request = new PaymentInitiateRequest(PaymentMethod.UPI, "IDEMP-FORGE");

        assertThatThrownBy(() -> paymentService.initiatePayment(10L, 100L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("authenticated customer");
    }

    @Test
    @DisplayName("Should strictly derive checkoutReference from server order reservation without trusting client input")
    void testInitiatePayment_ServerDerivedCheckoutReference() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        StockReservation validRes = new StockReservation("RES-VALID", customer, "CHK-SERVER-DERIVED");
        validRes.setStatus(ReservationStatus.ACTIVE);
        validRes.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));

        // Attach reservation item with order's product
        com.team5.onlineshopping.inventory.entity.StockReservationItem item =
                new com.team5.onlineshopping.inventory.entity.StockReservationItem(validRes, product, 1);
        validRes.setItems(List.of(item));

        when(stockReservationRepository.findByCustomerId(10L)).thenReturn(List.of(validRes));

        PaymentGateway.GatewayInitiateResult gwResult = new PaymentGateway.GatewayInitiateResult(
                "PAY-REF-SAFE", "MOCK-GW-SAFE", PaymentStatus.PENDING, "https://mock.pay/PAY-REF-SAFE");
        when(paymentGateway.initiatePayment(eq(100L), eq("ORD-123"), any(), any(), any())).thenReturn(gwResult);

        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentInitiateRequest request = new PaymentInitiateRequest(PaymentMethod.UPI, "IDEMP-SAFE");
        PaymentInitiateResponse response = paymentService.initiatePayment(10L, 100L, request);

        assertThat(response.getPaymentReference()).isEqualTo("PAY-REF-SAFE");
        org.mockito.ArgumentCaptor<PaymentTransaction> captor = org.mockito.ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(captor.capture());
        assertThat(captor.getValue().getCheckoutReference()).isEqualTo("CHK-SERVER-DERIVED");
    }
}
