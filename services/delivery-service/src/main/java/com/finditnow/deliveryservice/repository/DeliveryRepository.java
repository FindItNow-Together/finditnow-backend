package com.finditnow.deliveryservice.repository;

import com.finditnow.deliveryservice.entity.Delivery;
import com.finditnow.deliveryservice.entity.DeliveryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByOrderId(UUID orderId);

    Page<Delivery> findByAssignedAgentIdAndStatus(
            UUID agentId, DeliveryStatus status, Pageable pageable);

    Page<Delivery> findByAssignedAgentIdAndStatusNotIn(
            UUID agentId, List<DeliveryStatus> statuses, Pageable pageable);

    /**
     * Finds the oldest CREATED delivery and locks it for assignment.
     *
     * This prevents multiple agents from being assigned
     * to the same delivery under concurrent load.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT d
        FROM Delivery d
        WHERE d.status = :status
        ORDER BY d.createdAt ASC
        """)
    Optional<Delivery> findFirstCreatedForAssignment(
            @Param("status") DeliveryStatus status
    );
}

