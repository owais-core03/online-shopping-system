package com.team5.onlineshopping.checkout.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.onlineshopping.cart.entity.Cart;
import com.team5.onlineshopping.cart.entity.CartItem;
import com.team5.onlineshopping.cart.repository.CartRepository;
import com.team5.onlineshopping.checkout.dto.AddressDto;
import com.team5.onlineshopping.checkout.dto.CheckoutItemResponse;
import com.team5.onlineshopping.checkout.dto.CheckoutRequest;
import com.team5.onlineshopping.checkout.dto.CheckoutResponse;
import com.team5.onlineshopping.checkout.dto.CouponApplyResponse;
import com.team5.onlineshopping.common.enums.DiscountType;
import com.team5.onlineshopping.common.enums.OrderItemStatus;
import com.team5.onlineshopping.common.enums.OrderStatus;
import com.team5.onlineshopping.common.enums.PaymentStatus;
import com.team5.onlineshopping.common.enums.ReservationStatus;
import com.team5.onlineshopping.common.exception.BadRequestException;
import com.team5.onlineshopping.common.exception.ResourceNotFoundException;
import com.team5.onlineshopping.coupon.dto.CouponReservationResult;
import com.team5.onlineshopping.coupon.dto.CouponValidationResult;
import com.team5.onlineshopping.coupon.entity.Coupon;
import com.team5.onlineshopping.coupon.repository.CouponRepository;
import com.team5.onlineshopping.coupon.service.CouponService;
import com.team5.onlineshopping.inventory.dto.ReservationItem;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CheckoutServiceImpl implements CheckoutService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final InventoryService inventoryService;
    private final CouponService couponService;
    private final ObjectMapper objectMapper;

    public CheckoutServiceImpl(
            CartRepository cartRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            CouponRepository couponRepository,
            @Autowired(required = false) InventoryService inventoryService,
            @Autowired(required = false) CouponService couponService,
            ObjectMapper objectMapper) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.couponRepository = couponRepository;
        this.inventoryService = inventoryService;
        this.couponService = couponService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public CouponApplyResponse validateCartCoupon(Long customerId, String couponCode) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            throw new BadRequestException("Coupon code is required");
        }

        Cart cart = cartRepository.findByCustomerIdWithItems(customerId)
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        BigDecimal verifiedSubtotal = calculateCartSubtotal(cart);

        BigDecimal discountAmount = BigDecimal.ZERO;
        String discountTypeStr = "UNKNOWN";

        if (couponService != null) {
            CouponValidationResult result = couponService.validateCoupon(couponCode, customerId, verifiedSubtotal);
            if (!result.isValid()) {
                throw new BadRequestException("Invalid coupon: " + result.getMessage());
            }
            discountAmount = result.getDiscountAmount();
            Coupon coupon = couponRepository.findByCode(couponCode).orElse(null);
            if (coupon != null && coupon.getDiscountType() != null) {
                discountTypeStr = coupon.getDiscountType().name();
            }
        } else {
            // Standalone fallback: validate against coupon entity directly
            Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(couponCode)
                    .orElseThrow(() -> new BadRequestException("Invalid or inactive coupon code"));

            Instant now = Instant.now();
            if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
                throw new BadRequestException("Coupon has not started yet");
            }
            if (coupon.getValidUntil() != null && now.isAfter(coupon.getValidUntil())) {
                throw new BadRequestException("Coupon has expired");
            }
            if (coupon.getMaxUsageCount() != null && coupon.getCurrentUsageCount() >= coupon.getMaxUsageCount()) {
                throw new BadRequestException("Coupon usage limit reached");
            }
            if (coupon.getMinimumOrderAmount() != null && verifiedSubtotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
                throw new BadRequestException("Order amount of " + verifiedSubtotal + " is below minimum order amount of " + coupon.getMinimumOrderAmount());
            }

            if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
                discountAmount = verifiedSubtotal.multiply(coupon.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                if (coupon.getMaximumDiscountAmount() != null && discountAmount.compareTo(coupon.getMaximumDiscountAmount()) > 0) {
                    discountAmount = coupon.getMaximumDiscountAmount();
                }
            } else if (coupon.getDiscountType() == DiscountType.FIXED_AMOUNT) {
                discountAmount = coupon.getDiscountValue();
            }

            discountTypeStr = coupon.getDiscountType() != null ? coupon.getDiscountType().name() : "FIXED_AMOUNT";
        }

        discountAmount = discountAmount.min(verifiedSubtotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalPayable = verifiedSubtotal.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);

        return new CouponApplyResponse(
                couponCode,
                discountTypeStr,
                discountAmount,
                verifiedSubtotal,
                finalPayable
        );
    }

    @Override
    public CheckoutResponse processCheckout(Long customerId, CheckoutRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        Cart cart = cartRepository.findByCustomerIdWithItems(customerId)
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // Validate products, stock, active status
        List<CartItem> sortedCartItems = new ArrayList<>(cart.getItems());
        sortedCartItems.sort(Comparator.comparing(item -> item.getProduct().getId()));

        for (CartItem item : sortedCartItems) {
            Product currentProduct = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + item.getProduct().getId()));

            if (!currentProduct.isActive()) {
                throw new BadRequestException("Product '" + currentProduct.getName() + "' is no longer active");
            }

            if (currentProduct.getAvailableStock() < item.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for product '" + currentProduct.getName() +
                        "'. Requested: " + item.getQuantity() + ", Available: " + currentProduct.getAvailableStock());
            }
        }

        BigDecimal subtotal = calculateCartSubtotal(cart);
        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String appliedCouponCode = null;

        String checkoutReference = "CHK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        // 1. Coupon validation and reservation if couponCode supplied
        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            appliedCouponCode = request.getCouponCode().trim();
            if (couponService != null) {
                CouponValidationResult validation = couponService.validateCoupon(appliedCouponCode, customerId, subtotal);
                if (!validation.isValid()) {
                    throw new BadRequestException("Invalid coupon: " + validation.getMessage());
                }
                discountAmount = validation.getDiscountAmount().min(subtotal).setScale(2, RoundingMode.HALF_UP);

                CouponReservationResult resResult = couponService.reserveCoupon(appliedCouponCode, customerId, checkoutReference);
                if (!resResult.isReserved()) {
                    throw new BadRequestException("Failed to reserve coupon: " + resResult.getMessage());
                }
            } else {
                CouponApplyResponse couponValidation = validateCartCoupon(customerId, appliedCouponCode);
                discountAmount = couponValidation.getDiscountAmount();
            }
        }

        // 2. Stock reservation via InventoryService contract
        if (inventoryService == null) {
            if (couponService != null && appliedCouponCode != null) {
                try {
                    couponService.releaseCouponReservation(checkoutReference);
                } catch (Exception ignored) {
                }
            }
            throw new IllegalStateException("Inventory reservation service is currently unavailable. Checkout cannot proceed without stock reservation.");
        }

        String reservationCode;
        Instant reservationExpiresAt;

        List<ReservationItem> reservationItems = new ArrayList<>();
        for (CartItem item : sortedCartItems) {
            reservationItems.add(new ReservationItem(item.getProduct().getId(), item.getQuantity()));
        }

        StockReservationRequest stockRequest = new StockReservationRequest(checkoutReference, customerId, reservationItems);
        try {
            StockReservationResult stockResult = inventoryService.reserveStock(stockRequest);
            if (stockResult == null || stockResult.getStatus() != ReservationStatus.ACTIVE) {
                throw new IllegalStateException("Stock reservation failed or was not confirmed as ACTIVE");
            }
            reservationCode = stockResult.getReservationCode() != null
                    ? stockResult.getReservationCode()
                    : "RES-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
            reservationExpiresAt = stockResult.getExpiresAt() != null
                    ? stockResult.getExpiresAt()
                    : Instant.now().plus(15, ChronoUnit.MINUTES);
        } catch (Exception ex) {
            // If stock reservation fails, safely release coupon if reserved
            if (couponService != null && appliedCouponCode != null) {
                try {
                    couponService.releaseCouponReservation(checkoutReference);
                } catch (Exception ignored) {
                }
            }
            throw ex;
        }

        // 3. Address snapshot serialization
        String shippingAddressJson;
        try {
            shippingAddressJson = objectMapper.writeValueAsString(request.getDeliveryAddress());
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid delivery address structure");
        }

        // 4. Calculate Order and OrderItem amounts with proportional discount allocation
        BigDecimal shippingFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.subtract(discountAmount).add(shippingFee).setScale(2, RoundingMode.HALF_UP);

        String orderNumber = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        Order order = new Order(
                orderNumber,
                customer,
                subtotal,
                discountAmount,
                shippingFee,
                totalAmount,
                request.getPaymentMethod(),
                shippingAddressJson
        );
        order.setCouponCode(appliedCouponCode);
        order.setOrderStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING);

        // Item-level discount allocation
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal accumulatedAllocatedDiscount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (int i = 0; i < sortedCartItems.size(); i++) {
            CartItem cartItem = sortedCartItems.get(i);
            Product product = cartItem.getProduct();
            BigDecimal unitPrice = product.getPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal grossAmount = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())).setScale(2, RoundingMode.HALF_UP);

            BigDecimal lineAllocatedDiscount;
            if (discountAmount.compareTo(BigDecimal.ZERO) == 0 || subtotal.compareTo(BigDecimal.ZERO) == 0) {
                lineAllocatedDiscount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else if (i == sortedCartItems.size() - 1) {
                // Last item absorbs rounding difference to ensure sum(allocatedDiscount) == discountAmount exactly
                lineAllocatedDiscount = discountAmount.subtract(accumulatedAllocatedDiscount).setScale(2, RoundingMode.HALF_UP);
            } else {
                BigDecimal lineWeight = grossAmount.divide(subtotal, 8, RoundingMode.HALF_UP);
                lineAllocatedDiscount = discountAmount.multiply(lineWeight).setScale(2, RoundingMode.HALF_UP);
                accumulatedAllocatedDiscount = accumulatedAllocatedDiscount.add(lineAllocatedDiscount);
            }

            BigDecimal netPaidAmount = grossAmount.subtract(lineAllocatedDiscount).setScale(2, RoundingMode.HALF_UP);

            OrderItem orderItem = new OrderItem(
                    order,
                    product,
                    product.getSeller(),
                    cartItem.getQuantity(),
                    unitPrice,
                    lineAllocatedDiscount
            );
            orderItem.setNetPaidAmount(netPaidAmount);
            orderItem.setItemStatus(OrderItemStatus.PENDING_SELLER_ACCEPTANCE);
            order.addItem(orderItem);
            orderItems.add(orderItem);
        }

        // Persist order with cascading items
        Order savedOrder = orderRepository.save(order);

        // Clear cart upon successful checkout creation
        cart.clearItems();
        cartRepository.save(cart);

        // Build CheckoutResponse
        CheckoutResponse response = new CheckoutResponse();
        response.setOrderId(savedOrder.getId());
        response.setOrderNumber(savedOrder.getOrderNumber());
        response.setCheckoutReference(checkoutReference);
        response.setReservationCode(reservationCode);
        response.setReservationExpiresAt(reservationExpiresAt);
        response.setOrderStatus(savedOrder.getOrderStatus());
        response.setPaymentStatus(savedOrder.getPaymentStatus());
        response.setPaymentMethod(savedOrder.getPaymentMethod());
        response.setSubtotalAmount(savedOrder.getSubtotalAmount());
        response.setDiscountAmount(savedOrder.getDiscountAmount());
        response.setCouponCode(savedOrder.getCouponCode());
        response.setShippingFee(savedOrder.getShippingFee());
        response.setTotalAmount(savedOrder.getTotalAmount());
        response.setDeliveryAddress(request.getDeliveryAddress());
        response.setCreatedAt(savedOrder.getCreatedAt() != null ? savedOrder.getCreatedAt() : Instant.now());

        List<CheckoutItemResponse> itemResponses = new ArrayList<>();
        for (OrderItem oi : savedOrder.getItems()) {
            CheckoutItemResponse itemResp = new CheckoutItemResponse();
            itemResp.setOrderItemId(oi.getId());
            itemResp.setProductId(oi.getProduct().getId());
            itemResp.setProductName(oi.getProduct().getName());
            itemResp.setQuantity(oi.getQuantity());
            itemResp.setUnitPrice(oi.getUnitPrice());
            itemResp.setGrossAmount(oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity())).setScale(2, RoundingMode.HALF_UP));
            itemResp.setAllocatedDiscount(oi.getAllocatedDiscount());
            itemResp.setNetPaidAmount(oi.getNetPaidAmount());
            itemResponses.add(itemResp);
        }
        response.setItems(itemResponses);

        return response;
    }

    private BigDecimal calculateCartSubtotal(Cart cart) {
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (CartItem item : cart.getItems()) {
            BigDecimal itemTotal = item.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(itemTotal);
        }
        return subtotal;
    }
}
