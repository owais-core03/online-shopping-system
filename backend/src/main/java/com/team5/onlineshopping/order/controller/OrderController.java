package com.team5.onlineshopping.order.controller;

import com.team5.onlineshopping.common.dto.ApiResponse;
import com.team5.onlineshopping.order.dto.OrderDetailsResponse;
import com.team5.onlineshopping.order.service.OrderService;
import com.team5.onlineshopping.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDetailsResponse>>> getOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<OrderDetailsResponse> orders = orderService.getCustomerOrders(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDetailsResponse>> getOrderById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        OrderDetailsResponse order = orderService.getCustomerOrderById(userDetails.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Order details retrieved successfully", order));
    }
}
