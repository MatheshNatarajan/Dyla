package com.ticketstampede.repository;

import com.ticketstampede.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {
    boolean existsByRequestId(String requestId);
    long countByStatus(Purchase.PurchaseStatus status);
}
