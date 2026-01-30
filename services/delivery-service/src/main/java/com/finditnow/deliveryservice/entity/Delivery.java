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
        name = "deliveries",
        indexes = {
                @Index(name = "idx_delivery_order", columnList = "orderId"),
                @Index(name = "idx_delivery_agent", columnList = "assignedAgentId"),
                @Index(name = "idx_delivery_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "orderId")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID orderId;

    @Column(nullable = false, updatable = false)
    private Long shopId;

    @Column(nullable = false, updatable = false)
    private UUID customerId;

    @Column
    private UUID assignedAgentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryType type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String pickupAddress;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String deliveryAddress;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column
    private Double deliveryCharge;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

