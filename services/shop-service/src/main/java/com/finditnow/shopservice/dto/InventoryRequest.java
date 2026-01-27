package com.finditnow.shopservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryRequest {
    long inventoryId;

    int reservedStock; //already reserved (in someone's cart)

    float price;

    int stock;

    ShopResponse shop;

    ProductResponse product;
}
