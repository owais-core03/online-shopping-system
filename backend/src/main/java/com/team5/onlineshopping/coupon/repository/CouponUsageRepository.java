package com.team5.onlineshopping.coupon.repository;

import com.team5.onlineshopping.coupon.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    long countByCouponIdAndCustomerId(Long couponId, Long customerId);

    List<CouponUsage> findByCustomerId(Long customerId);

    List<CouponUsage> findByCouponId(Long couponId);
}
