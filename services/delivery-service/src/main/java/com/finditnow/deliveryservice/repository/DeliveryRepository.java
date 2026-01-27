package com.finditnow.deliveryservice.repository;

import com.finditnow.deliveryservice.entity.Delivery;
import com.finditnow.deliveryservice.entity.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
