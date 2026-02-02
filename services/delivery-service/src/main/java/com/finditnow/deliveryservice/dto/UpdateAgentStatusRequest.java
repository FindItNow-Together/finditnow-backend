package com.finditnow.deliveryservice.dto;

import com.finditnow.deliveryservice.entity.DeliveryAgentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAgentStatusRequest {
    @NotNull
    private DeliveryAgentStatus status;
}