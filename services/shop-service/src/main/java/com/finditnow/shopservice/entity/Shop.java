package com.finditnow.shopservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shop")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String phone;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "open_hours", nullable = false)
    private String openHours;

    /**
     * Stores the delivery option chosen by the shop owner.
     * Expected values (can be extended):
     * - NO_DELIVERY
     * - IN_HOUSE_DRIVER
     * - THIRD_PARTY_PARTNER
     */
    @Column(name = "delivery_option", nullable = false)
    private String deliveryOption;

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products = new ArrayList<>();
}

