package com.finditnow.deliveryservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateDeliveryAgentRequest {
    @NotNull
    private UUID agentId;
    private String zone;
}
