package com.finditnow.orderservice.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CartItemDTO {
    @JsonProperty("itemId")
    private UUID id;
    private Long productId;
    private String productName;
    private Double price;
    private Integer quantity;
}
