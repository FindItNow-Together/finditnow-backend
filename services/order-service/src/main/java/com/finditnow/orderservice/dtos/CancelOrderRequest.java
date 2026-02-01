package com.finditnow.orderservice.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelOrderRequest {

    @NotBlank(message = "Cancellation reason is required")
    @Size(min = 5, message = "Cancellation reason must be at least 5 characters")
    private String reason;
}
