package com.finditnow.deliveryservice.dto;

import com.finditnow.deliveryservice.entity.DeliveryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateRequest {
    @NotNull(message = "Status is required")
    private DeliveryStatus status;
}