package com.finditnow.shopservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
    private UUID ownerId;

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
    private Set<ShopInventory> shopInventory = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "image_url")
    private String imageUrl;

    public void addProduct(Product product, int stock, Float price) {
        boolean exists = shopInventory.stream().anyMatch(i -> i.getProduct().equals(product));

        if (exists) {
            throw new IllegalStateException("Product already exists in inventory");
        }

        ShopInventory newInventory = new ShopInventory();
        newInventory.setProduct(product);
        newInventory.setShop(this);
        newInventory.setStock(stock);
        newInventory.setPrice(price);
        newInventory.setReservedStock(0);
        shopInventory.add(newInventory);
    }

    public void removeProduct(Product product) {
        shopInventory.removeIf(i -> i.getProduct().equals(product));
    }
}

