package com.team5.onlineshopping.postpurchase.repository;

import com.team5.onlineshopping.postpurchase.entity.RefundTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundTransactionRepository extends JpaRepository<RefundTransaction, Long> {

    Optional<RefundTransaction> findByRefundCode(String refundCode);

    List<RefundTransaction> findByOrderId(Long orderId);

    Optional<RefundTransaction> findByReturnRequestId(Long returnRequestId);
}
