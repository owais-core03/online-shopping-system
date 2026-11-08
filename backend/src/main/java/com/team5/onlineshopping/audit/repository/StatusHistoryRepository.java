package com.team5.onlineshopping.audit.repository;

import com.team5.onlineshopping.audit.entity.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

    List<StatusHistory> findByEntityTypeAndEntityIdOrderByChangedAtDesc(String entityType, Long entityId);
}
