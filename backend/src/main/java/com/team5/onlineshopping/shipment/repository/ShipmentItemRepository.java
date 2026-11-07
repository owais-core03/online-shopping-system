package com.team5.onlineshopping.shipment.repository;

import com.team5.onlineshopping.shipment.entity.ShipmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentItemRepository extends JpaRepository<ShipmentItem, Long> {

    List<ShipmentItem> findByShipmentId(Long shipmentId);

    List<ShipmentItem> findByOrderItemId(Long orderItemId);
}
