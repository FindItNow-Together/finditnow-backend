package com.finditnow.deliveryservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "delivery_agents",
        indexes = {
                @Index(name = "idx_delivery_agent_status", columnList = "status")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAgent {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID agentId; // SAME as users.id

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryAgentStatus status;

    @Column
    private UUID currentDeliveryId;

    @Column(length = 50)
    private String zone; // optional now, powerful later

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
