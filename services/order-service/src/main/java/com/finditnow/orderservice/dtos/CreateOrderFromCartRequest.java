package com.finditnow.orderservice.dtos;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateOrderFromCartRequest {
    private UUID cartId;
    private UUID addressId;
    private String paymentMethod; // "online" or "cash_on_delivery"
<<<<<<< HEAD
=======
    private String instructions;
    private String deliveryType; // "PARTNER", "SELF", "TAKEAWAY"
>>>>>>> da72e1a (Update delivery system implementation)
}
