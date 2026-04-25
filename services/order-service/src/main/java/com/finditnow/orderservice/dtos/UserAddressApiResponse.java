package com.finditnow.orderservice.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class UserAddressApiResponse {
    private UserAddress data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserAddress {
        private UUID id;

        private String line1;

        private String line2;

        private String city;

        private String state;

        private String country;

        private String postalCode;

        private String addressType;

        private double latitude;
        private double longitude;

        @JsonProperty("isPrimary")
        private boolean isPrimary;

        private String fullAddress;
        private UUID userId;
    }
}


