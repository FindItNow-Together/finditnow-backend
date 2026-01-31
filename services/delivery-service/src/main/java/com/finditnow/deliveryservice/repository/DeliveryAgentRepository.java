package com.finditnow.deliveryservice.repository;

import com.finditnow.deliveryservice.entity.DeliveryAgent;
import com.finditnow.deliveryservice.entity.DeliveryAgentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryAgentRepository extends JpaRepository<DeliveryAgent, UUID> {

    /**
     * Finds the oldest AVAILABLE delivery agent and locks it for assignment.
     *
     * This method is used during delivery assignment and MUST prevent
     * concurrent threads from selecting the same agent.
     *
     * PESSIMISTIC_WRITE ensures exclusive access until the transaction completes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a
        FROM DeliveryAgent a
        WHERE a.status = :status
        ORDER BY a.createdAt ASC
        """)
    Optional<DeliveryAgent> findFirstAvailableForAssignment(
            @Param("status") DeliveryAgentStatus status
    );
}


