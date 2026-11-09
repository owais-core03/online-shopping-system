package com.team5.onlineshopping.payment.repository;

import com.team5.onlineshopping.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByPaymentReference(String paymentReference);

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);

    List<PaymentTransaction> findByOrderId(Long orderId);

    Optional<PaymentTransaction> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);
}
