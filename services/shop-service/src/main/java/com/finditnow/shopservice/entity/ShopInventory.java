package com.finditnow.shopservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shop_inventories")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShopInventory {
    @Id
    long id;

    @Min(0)
    int reservedStock; // already reserved (in someone's cart)

    @Min(0)
    float price;

    @Min(0)
    int stock;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id")
    Shop shop;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinColumn(name = "product_id")
    Product product;
}
