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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Min(0)
<<<<<<< HEAD
    private int reservedStock; //already reserved (in someone's cart)
=======
    int reservedStock; // already reserved (in someone's cart)
>>>>>>> 49c6c06b8ddd591a5e5d2dd8ed7431f333caf104

    @Min(0)
    private float price;

    @Min(0)
    private int stock;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id")
    private Shop shop;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinColumn(name = "product_id")
    private Product product;
}
