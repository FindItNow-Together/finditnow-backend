package com.finditnow.auth.types;

public enum Role {
    CUSTOMER, ADMIN, SHOP, DELIVERY_AGENT;

    public String toDb() {
        return name().toLowerCase();
    }

    public static Role fromDb(String value) {
        return Role.valueOf(value.toUpperCase());
    }
}
