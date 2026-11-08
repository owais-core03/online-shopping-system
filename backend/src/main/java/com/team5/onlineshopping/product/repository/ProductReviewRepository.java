package com.team5.onlineshopping.product.repository;

import com.team5.onlineshopping.product.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProductId(Long productId);

    List<ProductReview> findByProductIdAndIsApprovedTrue(Long productId);

    Optional<ProductReview> findByProductIdAndCustomerId(Long productId, Long customerId);

    Optional<ProductReview> findByOrderItemId(Long orderItemId);
}
