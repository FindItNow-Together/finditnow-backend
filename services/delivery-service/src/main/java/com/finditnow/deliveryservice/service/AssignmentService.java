package com.finditnow.deliveryservice.service;

import com.finditnow.deliveryservice.entity.Delivery;
import com.finditnow.deliveryservice.entity.DeliveryAgent;
import com.finditnow.deliveryservice.entity.DeliveryAgentStatus;
import com.finditnow.deliveryservice.entity.DeliveryStatus;
import com.finditnow.deliveryservice.repository.DeliveryAgentRepository;
import com.finditnow.deliveryservice.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;

    /**
     * Attempts to assign one AVAILABLE agent to one CREATED delivery.
     *
     * This method:
     * - Runs in the caller's transaction
     * - Uses pessimistic locking to prevent double assignment
     * - Is intentionally silent if assignment is not possible
     *
     * IMPORTANT:
     * Any exception thrown here should be swallowed by the caller,
     * as assignment is best-effort.
     */
    @Transactional
    public void attemptAssignment() {

        Optional<DeliveryAgent> agentOpt =
                deliveryAgentRepository.findFirstAvailableForAssignment(
                        DeliveryAgentStatus.AVAILABLE
                );

        Optional<Delivery> deliveryOpt =
                deliveryRepository.findFirstCreatedForAssignment(
                        DeliveryStatus.CREATED
                );

        if (agentOpt.isEmpty() || deliveryOpt.isEmpty()) {
            return;
        }

        DeliveryAgent agent = agentOpt.get();
        Delivery delivery = deliveryOpt.get();

        agent.setStatus(DeliveryAgentStatus.ASSIGNED);
        agent.setCurrentDeliveryId(delivery.getId());

        delivery.setAssignedAgentId(agent.getAgentId());
        delivery.setStatus(DeliveryStatus.ASSIGNED);

        deliveryAgentRepository.save(agent);
        deliveryRepository.save(delivery);

        log.info(
                "Assigned delivery {} to agent {}",
                delivery.getId(), agent.getAgentId()
        );
    }
}


