package com.team5.onlineshopping.shipment.repository;

import com.team5.onlineshopping.common.enums.DeliveryStatus;
import com.team5.onlineshopping.shipment.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByShipmentNumber(String shipmentNumber);

    List<Shipment> findByOrderId(Long orderId);

    List<Shipment> findBySellerId(Long sellerId);

    List<Shipment> findByDeliveryAgentId(Long deliveryAgentId);

    List<Shipment> findByDeliveryAgentIdAndDeliveryStatus(Long deliveryAgentId, DeliveryStatus deliveryStatus);
}
