package com.team5.onlineshopping.payment.service;

import com.team5.onlineshopping.audit.entity.StatusHistory;
import com.team5.onlineshopping.audit.repository.StatusHistoryRepository;
import com.team5.onlineshopping.common.enums.OrderItemStatus;
import com.team5.onlineshopping.common.enums.OrderStatus;
import com.team5.onlineshopping.common.enums.PaymentMethod;
import com.team5.onlineshopping.common.enums.PaymentStatus;
import com.team5.onlineshopping.common.enums.ReservationStatus;
import com.team5.onlineshopping.common.exception.BadRequestException;
import com.team5.onlineshopping.common.exception.ResourceNotFoundException;
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
import com.team5.onlineshopping.user.entity.User;
import com.team5.onlineshopping.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final StockReservationRepository stockReservationRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final PaymentGateway paymentGateway;
    private final InventoryService inventoryService;
    private final CouponService couponService;
    private final OrderStatusService orderStatusService;

    public PaymentServiceImpl(
            OrderRepository orderRepository,
            UserRepository userRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            StockReservationRepository stockReservationRepository,
            StatusHistoryRepository statusHistoryRepository,
            PaymentGateway paymentGateway,
            @Autowired(required = false) InventoryService inventoryService,
            @Autowired(required = false) CouponService couponService,
            OrderStatusService orderStatusService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.stockReservationRepository = stockReservationRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.paymentGateway = paymentGateway;
        this.inventoryService = inventoryService;
        this.couponService = couponService;
        this.orderStatusService = orderStatusService;
    }

    @Override
    public PaymentInitiateResponse initiatePayment(Long customerId, Long orderId, PaymentInitiateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // 1. Customer ownership check
        if (!order.getCustomer().getId().equals(customerId)) {
            throw new AccessDeniedException("You are not authorized to initiate payment for this order");
        }

        // 2. Order status eligibility check
        if (order.getPaymentStatus() == PaymentStatus.CAPTURED) {
            throw new BadRequestException("Order is already paid");
        }
        if (order.getOrderStatus() == OrderStatus.CANCELLED || order.getPaymentStatus() == PaymentStatus.FAILED) {
            throw new BadRequestException("Order is cancelled or payment previously failed and cannot be paid");
        }
        if (order.getOrderStatus() != OrderStatus.CREATED && order.getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new BadRequestException("Order is not in a payable state: " + order.getOrderStatus());
        }

        // 3. Idempotency check: if an existing pending transaction exists for this idempotency key
        if (request != null && request.getIdempotencyKey() != null && !request.getIdempotencyKey().trim().isEmpty()) {
            Optional<PaymentTransaction> existingTxOpt = paymentTransactionRepository.findByIdempotencyKey(request.getIdempotencyKey().trim());
            if (existingTxOpt.isPresent()) {
                PaymentTransaction existingTx = existingTxOpt.get();
                if (existingTx.getOrder().getId().equals(order.getId())) {
                    return mapToInitiateResponse(existingTx, order);
                } else {
                    throw new BadRequestException("Idempotency key already used for a different order");
                }
            }
        }

        // 4. Verify stock reservation validity and resolve original checkoutReference exclusively from server-side order & customer data
        List<StockReservation> activeReservations = stockReservationRepository.findByCustomerId(customerId).stream()
                .filter(res -> res.getStatus() == ReservationStatus.ACTIVE)
                .toList();

        Instant now = Instant.now();
        String resolvedCheckoutReference = null;

        // Collect product IDs in the current order
        java.util.Set<Long> orderProductIds = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .collect(java.util.stream.Collectors.toSet());

        for (StockReservation res : activeReservations) {
            // Verify reservation belongs to the authenticated customer
            if (!res.getCustomer().getId().equals(customerId)) {
                throw new AccessDeniedException("Reservation does not belong to the authenticated customer");
            }

            if (res.getExpiresAt().isBefore(now)) {
                res.setStatus(ReservationStatus.EXPIRED);
                stockReservationRepository.save(res);
                throw new IllegalStateException("Stock reservation has expired. Please create a new checkout.");
            }

            // Verify reservation contains items corresponding to this order
            boolean matchesOrder = res.getItems() != null && !res.getItems().isEmpty() &&
                    res.getItems().stream().anyMatch(item -> orderProductIds.contains(item.getProduct().getId()));

            if (matchesOrder && resolvedCheckoutReference == null) {
                resolvedCheckoutReference = res.getCheckoutReference();
            }
        }

        if (resolvedCheckoutReference == null && !activeReservations.isEmpty()) {
            resolvedCheckoutReference = activeReservations.getFirst().getCheckoutReference();
        }

        if (resolvedCheckoutReference == null) {
            // Check most recent reservation for customer as fallback
            Optional<StockReservation> latestRes = stockReservationRepository.findTopByCustomerIdOrderByCreatedAtDesc(customerId);
            if (latestRes.isPresent() && latestRes.get().getCustomer().getId().equals(customerId)) {
                resolvedCheckoutReference = latestRes.get().getCheckoutReference();
            }
        }

        // 5. Initiate through Gateway
        PaymentMethod method = (request != null && request.getPaymentMethod() != null)
                ? request.getPaymentMethod()
                : order.getPaymentMethod();

        String simulatedOutcome = request != null ? request.getSimulatedOutcome() : null;

        PaymentGateway.GatewayInitiateResult gwResult = paymentGateway.initiatePayment(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                method,
                simulatedOutcome
        );

        String idempotencyKey = (request != null && request.getIdempotencyKey() != null)
                ? request.getIdempotencyKey().trim()
                : "IDEMP-" + gwResult.paymentReference();

        PaymentTransaction transaction = new PaymentTransaction(
                gwResult.paymentReference(),
                order,
                order.getCustomer(),
                order.getTotalAmount(),
                method,
                idempotencyKey
        );
        transaction.setGatewayReference(gwResult.gatewayReference());
        transaction.setPaymentStatus(PaymentStatus.PENDING);
        transaction.setCheckoutReference(resolvedCheckoutReference);

        PaymentTransaction savedTx = paymentTransactionRepository.save(transaction);

        order.setPaymentStatus(PaymentStatus.PENDING);
        orderRepository.save(order);

        return mapToInitiateResponse(savedTx, order, gwResult.redirectUrl());
    }

    @Override
    public PaymentStatusResponse handlePaymentWebhook(PaymentWebhookRequest request) {
        if (request.getPaymentReference() == null || request.getPaymentReference().trim().isEmpty()) {
            throw new BadRequestException("Payment reference is required");
        }

        PaymentTransaction transaction = paymentTransactionRepository.findByPaymentReference(request.getPaymentReference().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found for reference: " + request.getPaymentReference()));

        // Idempotency: If transaction is already in terminal state, return existing state
        if (transaction.getPaymentStatus() == PaymentStatus.CAPTURED || transaction.getPaymentStatus() == PaymentStatus.FAILED) {
            return mapToStatusResponse(transaction);
        }

        // Server-side verification using Gateway (Never trust client raw status)
        PaymentGateway.GatewayVerifyResult verifyResult = paymentGateway.verifyPayment(
                transaction.getPaymentReference(),
                request.getSignature()
        );

        if (!verifyResult.verified()) {
            throw new BadRequestException("Payment webhook verification failed: " + verifyResult.failureReason());
        }

        Order order = transaction.getOrder();
        String checkoutRef = transaction.getCheckoutReference();

        if (verifyResult.status() == PaymentStatus.CAPTURED) {
            // STEP 5: SUCCESS WORKFLOW
            // Crucial: Confirm stock reservation and commit coupon BEFORE updating payment/order to CAPTURED/PROCESSING.
            // If confirmation fails, an exception is thrown and the transaction rolls back safely.
            if (inventoryService != null) {
                if (checkoutRef == null || checkoutRef.trim().isEmpty()) {
                    throw new IllegalStateException("Missing checkout reference for inventory confirmation");
                }
                inventoryService.confirmReservation(checkoutRef);
            }

            if (couponService != null && order.getCouponCode() != null) {
                if (checkoutRef != null && !checkoutRef.trim().isEmpty()) {
                    couponService.confirmCouponUsage(checkoutRef);
                }
            }

            transaction.setPaymentStatus(PaymentStatus.CAPTURED);
            transaction.setGatewayReference(verifyResult.gatewayReference());
            paymentTransactionRepository.save(transaction);

            // Advance Order & Item statuses
            PaymentStatus oldPaymentStatus = order.getPaymentStatus();
            OrderStatus oldOrderStatus = order.getOrderStatus();

            order.setPaymentStatus(PaymentStatus.CAPTURED);
            order.setOrderStatus(OrderStatus.PROCESSING);

            for (OrderItem item : order.getItems()) {
                if (item.getItemStatus() == OrderItemStatus.PENDING_SELLER_ACCEPTANCE) {
                    item.setItemStatus(OrderItemStatus.PROCESSING);
                }
            }

            orderRepository.save(order);

            // Status audit log
            statusHistoryRepository.save(new StatusHistory(
                    "Order",
                    order.getId(),
                    oldOrderStatus.name(),
                    OrderStatus.PROCESSING.name(),
                    order.getCustomer(),
                    "Payment captured successfully via reference " + transaction.getPaymentReference()
            ));

        } else {
            // STEP 6: FAILED PAYMENT WORKFLOW
            // Release reservations safely
            if (inventoryService != null && checkoutRef != null && !checkoutRef.trim().isEmpty()) {
                inventoryService.releaseReservation(checkoutRef);
            }

            if (couponService != null && order.getCouponCode() != null && checkoutRef != null && !checkoutRef.trim().isEmpty()) {
                couponService.releaseCouponReservation(checkoutRef);
            }

            transaction.setPaymentStatus(PaymentStatus.FAILED);
            transaction.setFailureReason(verifyResult.failureReason() != null ? verifyResult.failureReason() : request.getFailureReason());
            transaction.setGatewayReference(verifyResult.gatewayReference());
            paymentTransactionRepository.save(transaction);

            PaymentStatus oldPaymentStatus = order.getPaymentStatus();
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);

            statusHistoryRepository.save(new StatusHistory(
                    "Order",
                    order.getId(),
                    oldPaymentStatus.name(),
                    PaymentStatus.FAILED.name(),
                    order.getCustomer(),
                    "Payment failed: " + transaction.getFailureReason()
            ));
        }

        return mapToStatusResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(Long customerId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomer().getId().equals(customerId)) {
            throw new AccessDeniedException("You are not authorized to view payment details for this order");
        }

        PaymentTransaction transaction = paymentTransactionRepository.findTopByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment transaction found for order id: " + orderId));

        return mapToStatusResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentByReference(String paymentReference) {
        PaymentTransaction transaction = paymentTransactionRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found for reference: " + paymentReference));
        return mapToStatusResponse(transaction);
    }

    private PaymentInitiateResponse mapToInitiateResponse(PaymentTransaction tx, Order order) {
        return mapToInitiateResponse(tx, order, "https://mock-gateway.local/pay/" + tx.getPaymentReference());
    }

    private PaymentInitiateResponse mapToInitiateResponse(PaymentTransaction tx, Order order, String redirectUrl) {
        PaymentInitiateResponse response = new PaymentInitiateResponse();
        response.setPaymentReference(tx.getPaymentReference());
        response.setOrderId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setAmount(tx.getAmount());
        response.setPaymentMethod(tx.getPaymentMethod());
        response.setPaymentStatus(tx.getPaymentStatus());
        response.setRedirectUrl(redirectUrl);
        response.setCreatedAt(tx.getCreatedAt());
        return response;
    }

    private PaymentStatusResponse mapToStatusResponse(PaymentTransaction tx) {
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setPaymentReference(tx.getPaymentReference());
        response.setOrderId(tx.getOrder().getId());
        response.setOrderNumber(tx.getOrder().getOrderNumber());
        response.setAmount(tx.getAmount());
        response.setPaymentMethod(tx.getPaymentMethod());
        response.setPaymentStatus(tx.getPaymentStatus());
        response.setGatewayReference(tx.getGatewayReference());
        response.setFailureReason(tx.getFailureReason());
        response.setCreatedAt(tx.getCreatedAt());
        response.setUpdatedAt(tx.getUpdatedAt());
        return response;
    }
}
