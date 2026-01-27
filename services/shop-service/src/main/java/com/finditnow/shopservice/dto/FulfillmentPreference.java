package com.finditnow.shopservice.dto;

public enum FulfillmentPreference {
    DELIVERY, PICKUP, BOTH;

    public static FulfillmentPreference from(String value) {
        try {
            return FulfillmentPreference.valueOf(value.toUpperCase());
        } catch (Exception e) {
            return BOTH;
        }
    }

    public boolean allows(FulfillmentMode mode) {
        if (this == BOTH) return true;
        return this.name().equals(mode.name());
    }
}
