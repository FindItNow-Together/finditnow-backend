package com.finditnow.shopservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryResponse {
    long id;

    int reservedStock; //already reserved (in someone's cart)

    float price;

    int stock;

    ShopResponse shop;

    ProductResponse product;
}
