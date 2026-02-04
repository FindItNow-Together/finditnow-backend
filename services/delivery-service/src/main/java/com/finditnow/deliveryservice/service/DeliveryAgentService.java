package com.finditnow.deliveryservice.service;

import com.finditnow.deliveryservice.entity.DeliveryAgent;
import com.finditnow.deliveryservice.entity.DeliveryAgentStatus;
import com.finditnow.deliveryservice.repository.DeliveryAgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryAgentService {

    private final DeliveryAgentRepository agentRepository;
    private final AssignmentService assignmentService;

    /**
     * Creates a delivery agent if it does not already exist.
     * This operation is idempotent.
     */
    @Transactional
    public void createAgent(UUID agentId, String zone) {

        if (agentRepository.existsById(agentId)) {
            return;
        }

        DeliveryAgent agent = new DeliveryAgent();
        agent.setAgentId(agentId);
        agent.setStatus(DeliveryAgentStatus.OFFLINE);
        agent.setZone(zone);

        agentRepository.save(agent);
    }

    /**
     * Fetches a delivery agent by agent ID.
     *
     * This is a read-only operation and does not trigger
     * assignment or any side effects.
     */
    @Transactional(readOnly = true)
    public DeliveryAgent getAgent(UUID agentId) {
        return agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));
    }

    /**
     * Updates the agent status.
     *
     * If the agent becomes AVAILABLE, a best-effort attempt
     * is made to assign a delivery.
     *
     * Assignment failure MUST NOT roll back this transaction.
     */
    @Transactional
    public DeliveryAgentStatus updateStatus(UUID agentId, DeliveryAgentStatus newStatus) {

        DeliveryAgent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));

        DeliveryAgentStatus oldStatus = agent.getStatus();
        if (oldStatus == newStatus) {
            return newStatus;
        }

        agent.setStatus(newStatus);
        agent.setCurrentDeliveryId(null);

        agentRepository.save(agent);

        if (newStatus == DeliveryAgentStatus.AVAILABLE) {
            try {
                assignmentService.attemptAssignment();
            } catch (Exception e) {
                log.error(
                        "Delivery assignment failed for agent {}, continuing",
                        agentId, e
                );
            }
        }

        return newStatus;
    }

    /**
     * Returns the current status of the agent.
     */
    @Transactional(readOnly = true)
    public DeliveryAgentStatus getAgentStatus(UUID agentId) {
        return agentRepository.findById(agentId)
                .orElseThrow(() -> new NoSuchElementException("Agent not found"))
                .getStatus();
    }
}
