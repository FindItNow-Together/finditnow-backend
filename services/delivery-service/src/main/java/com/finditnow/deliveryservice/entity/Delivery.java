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
@Table(name = "deliveries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private Long shopId;

    @Column(nullable = false)
    private UUID customerId;

    private UUID assignedAgentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryType type;

    @Column(columnDefinition = "TEXT")
    private String pickupAddress; // JSON or formatted string

    @Column(columnDefinition = "TEXT")
    private String deliveryAddress; // JSON or formatted string

    private String instructions;

    private Double deliveryCharge;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

