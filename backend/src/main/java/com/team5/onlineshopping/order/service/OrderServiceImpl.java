package com.team5.onlineshopping.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.onlineshopping.checkout.dto.AddressDto;
import com.team5.onlineshopping.common.exception.ResourceNotFoundException;
import com.team5.onlineshopping.order.dto.OrderDetailsResponse;
import com.team5.onlineshopping.order.dto.OrderItemResponse;
import com.team5.onlineshopping.order.entity.Order;
import com.team5.onlineshopping.order.entity.OrderItem;
import com.team5.onlineshopping.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public OrderServiceImpl(OrderRepository orderRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<OrderDetailsResponse> getCustomerOrders(Long customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        List<OrderDetailsResponse> responses = new ArrayList<>();
        for (Order order : orders) {
            responses.add(mapToResponse(order));
        }
        return responses;
    }

    @Override
    public OrderDetailsResponse getCustomerOrderById(Long customerId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Strict customer ownership check
        if (order.getCustomer() == null || !order.getCustomer().getId().equals(customerId)) {
            // Return safe 404 to avoid leaking existence of orders belonging to other customers
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        return mapToResponse(order);
    }

    private OrderDetailsResponse mapToResponse(Order order) {
        OrderDetailsResponse resp = new OrderDetailsResponse();
        resp.setId(order.getId());
        resp.setOrderNumber(order.getOrderNumber());
        if (order.getCustomer() != null) {
            resp.setCustomerId(order.getCustomer().getId());
        }
        resp.setSubtotalAmount(order.getSubtotalAmount());
        resp.setDiscountAmount(order.getDiscountAmount());
        resp.setShippingFee(order.getShippingFee());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setCouponCode(order.getCouponCode());
        resp.setOrderStatus(order.getOrderStatus());
        resp.setPaymentStatus(order.getPaymentStatus());
        resp.setPaymentMethod(order.getPaymentMethod());
        resp.setCreatedAt(order.getCreatedAt());
        resp.setUpdatedAt(order.getUpdatedAt());

        // Parse JSON shipping address if available
        if (order.getShippingAddress() != null && !order.getShippingAddress().isBlank()) {
            try {
                AddressDto addr = objectMapper.readValue(order.getShippingAddress(), AddressDto.class);
                resp.setDeliveryAddress(addr);
            } catch (Exception e) {
                log.warn("Failed to deserialize shipping address for order id {}: {}", order.getId(), e.getMessage());
            }
        }

        // Map items
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem oi : order.getItems()) {
                OrderItemResponse ir = new OrderItemResponse();
                ir.setId(oi.getId());
                if (oi.getProduct() != null) {
                    ir.setProductId(oi.getProduct().getId());
                    ir.setProductName(oi.getProduct().getName());
                }
                ir.setQuantity(oi.getQuantity());
                ir.setUnitPrice(oi.getUnitPrice());
                if (oi.getUnitPrice() != null && oi.getQuantity() != null) {
                    ir.setGrossAmount(oi.getUnitPrice().multiply(BigDecimal.valueOf(oi.getQuantity())).setScale(2, RoundingMode.HALF_UP));
                }
                ir.setAllocatedDiscount(oi.getAllocatedDiscount());
                ir.setNetPaidAmount(oi.getNetPaidAmount());
                if (oi.getItemStatus() != null) {
                    ir.setItemStatus(oi.getItemStatus().name());
                }
                itemResponses.add(ir);
            }
        }
        resp.setItems(itemResponses);

        return resp;
    }
}
