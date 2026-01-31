package com.finditnow.deliveryservice.dto;

import com.finditnow.deliveryservice.entity.DeliveryAgentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateAgentStatusRequest {
    @NotNull
    private DeliveryAgentStatus status;
}