package com.finditnow.userservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Builder
@Data
public class CreateDeliveryAgentRequest {
    private UUID agentId;
    private String zone;
}

