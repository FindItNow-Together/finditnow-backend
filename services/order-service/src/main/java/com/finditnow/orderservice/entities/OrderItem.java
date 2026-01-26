package com.finditnow.orderservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "price_at_order")
    private Double priceAtOrder;

    @Column(name = "quantity")
    private Integer quantity;

    @PrePersist
    @PreUpdate
    private void validateQuantity() {
        if (quantity != null && quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
    }
}