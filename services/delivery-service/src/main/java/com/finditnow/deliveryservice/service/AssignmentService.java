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
import org.springframework.transaction.annotation.Transactional;

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
         *
         * This method:
         * - Runs in the caller's transaction
         * - Uses pessimistic locking to prevent double assignment
         * - Excludes deliveries the agent has opted out of
         * - Is intentionally silent if assignment is not possible
         *
         * IMPORTANT:
         * Any exception thrown here should be swallowed by the caller,
         * as assignment is best-effort.
         */
        @Transactional
        public void attemptAssignment() {

                Optional<DeliveryAgent> agentOpt = deliveryAgentRepository.findFirstAvailableForAssignment(
                                DeliveryAgentStatus.AVAILABLE);

                if (agentOpt.isEmpty()) {
                        return;
                }

                DeliveryAgent agent = agentOpt.get();

                // Find delivery available for this specific agent (excluding opted-out ones)
                Optional<Delivery> deliveryOpt = deliveryRepository.findFirstAvailableForAssignment(agent.getAgentId());

                if (deliveryOpt.isEmpty()) {
                        return;
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
}
