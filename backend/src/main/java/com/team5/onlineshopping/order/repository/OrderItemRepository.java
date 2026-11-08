package com.team5.onlineshopping.order.repository;

import com.team5.onlineshopping.common.enums.OrderItemStatus;
import com.team5.onlineshopping.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findBySellerId(Long sellerId);

    List<OrderItem> findBySellerIdAndItemStatus(Long sellerId, OrderItemStatus itemStatus);
}
