package com.team5.onlineshopping.inventory.repository;

import com.team5.onlineshopping.inventory.entity.StockReservationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockReservationItemRepository extends JpaRepository<StockReservationItem, Long> {

    List<StockReservationItem> findByReservationId(Long reservationId);

    List<StockReservationItem> findByProductId(Long productId);
}
