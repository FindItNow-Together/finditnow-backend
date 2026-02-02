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

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryAgentRepository deliveryAgentRepository;

    /**
     * Attempts to assign one AVAILABLE agent to one available delivery (CREATED or
     * UNASSIGNED).
     * <p>
     * This method:
     * - Runs in the caller's transaction
     * - Uses pessimistic locking to prevent double assignment
     * - Excludes deliveries the agent has opted out of
     * - Is intentionally silent if assignment is not possible
     * <p>
     * IMPORTANT:
     * Any exception thrown here should be swallowed by the caller,
     * as assignment is best-effort.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void attemptAssignmentOld() {

        Optional<DeliveryAgent> agentOpt = deliveryAgentRepository.findFirstAvailableForAssignment(
                DeliveryAgentStatus.AVAILABLE);

        if (agentOpt.isEmpty()) {
            throw new RuntimeException("No available delivery agents found");
        }

        DeliveryAgent agent = agentOpt.get();

        // Find delivery available for this specific agent (excluding opted-out ones)
        Optional<Delivery> deliveryOpt = deliveryRepository.findFirstAvailableForAssignment(agent.getAgentId());

        if (deliveryOpt.isEmpty()) {
            throw new RuntimeException("No deliveries found for agent " + agent.getAgentId() + " where agent is not in opted out mode");
        }

        Delivery delivery = deliveryOpt.get();

        agent.setStatus(DeliveryAgentStatus.ASSIGNED);
        agent.setCurrentDeliveryId(delivery.getId());

        delivery.setAssignedAgentId(agent.getAgentId());
        delivery.setStatus(DeliveryStatus.PENDING_ACCEPTANCE); // Agent must explicitly accept

        deliveryAgentRepository.save(agent);
        deliveryRepository.save(delivery);

        log.info(
                "Assigned delivery {} to agent {} (PENDING_ACCEPTANCE)",
                delivery.getId(), agent.getAgentId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void attemptAssignment() {
        List<DeliveryAgent> availableAgents = deliveryAgentRepository.findAllAvailableForAssignment(
                DeliveryAgentStatus.AVAILABLE
        );

        if (availableAgents.isEmpty()) {
            log.warn("No available delivery agents found");
            return;
        }

        // Fetch ALL unassigned deliveries at once
        List<Delivery> availableDeliveries = deliveryRepository.findAllAvailableForAssignment();

        if (availableDeliveries.isEmpty()) {
            log.warn("No deliveries available for assignment");
            return;
        }

        int assignmentCount = 0;

        for (DeliveryAgent agent : availableAgents) {
            // Find first delivery this agent hasn't opted out of
            Optional<Delivery> deliveryOpt = availableDeliveries.stream()
                    .filter(d -> !d.getOptedOutAgentIds().contains(agent.getAgentId()))
                    .findFirst();

            if (deliveryOpt.isEmpty()) {
                continue;
            }

            Delivery delivery = deliveryOpt.get();

            // Remove from available pool
            availableDeliveries.remove(delivery);

            // Assign
            agent.setStatus(DeliveryAgentStatus.ASSIGNED);
            agent.setCurrentDeliveryId(delivery.getId());

            delivery.setAssignedAgentId(agent.getAgentId());
            delivery.setStatus(DeliveryStatus.PENDING_ACCEPTANCE);

            assignmentCount++;

            log.info("Assigned delivery {} to agent {}", delivery.getId(), agent.getAgentId());
        }

        // Batch save
        deliveryAgentRepository.saveAll(availableAgents.stream()
                .filter(a -> a.getStatus() == DeliveryAgentStatus.ASSIGNED)
                .toList());

        deliveryRepository.saveAll(availableDeliveries.stream()
                .filter(d -> d.getAssignedAgentId() != null)
                .toList());

        log.info("Bulk assignment completed: {} deliveries assigned", assignmentCount);
    }
}
