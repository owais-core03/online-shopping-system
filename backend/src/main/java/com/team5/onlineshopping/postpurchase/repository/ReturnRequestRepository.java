package com.team5.onlineshopping.postpurchase.repository;

import com.team5.onlineshopping.postpurchase.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    Optional<ReturnRequest> findByReturnCode(String returnCode);

    List<ReturnRequest> findByCustomerId(Long customerId);

    List<ReturnRequest> findBySellerId(Long sellerId);

    List<ReturnRequest> findByOrderItemId(Long orderItemId);
}
