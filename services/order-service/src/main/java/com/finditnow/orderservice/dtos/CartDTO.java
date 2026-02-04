package com.finditnow.orderservice.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CartDTO {
    @JsonProperty("cartId")
    private UUID id;
    private UUID userId;
    private Long shopId;

    @JsonProperty("items")
    private List<CartItemDTO> items;
}
