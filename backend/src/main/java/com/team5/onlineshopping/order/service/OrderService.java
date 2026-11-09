package com.team5.onlineshopping.order.service;

import com.team5.onlineshopping.order.dto.OrderDetailsResponse;

import java.util.List;

public interface OrderService {

    List<OrderDetailsResponse> getCustomerOrders(Long customerId);

    OrderDetailsResponse getCustomerOrderById(Long customerId, Long orderId);
}
