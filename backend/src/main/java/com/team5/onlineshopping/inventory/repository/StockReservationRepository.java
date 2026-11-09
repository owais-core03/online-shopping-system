package com.team5.onlineshopping.inventory.repository;

import com.team5.onlineshopping.common.enums.ReservationStatus;
import com.team5.onlineshopping.inventory.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    Optional<StockReservation> findByReservationCode(String reservationCode);

    List<StockReservation> findByCustomerId(Long customerId);

    Optional<StockReservation> findByCheckoutReference(String checkoutReference);

    Optional<StockReservation> findTopByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);
}
