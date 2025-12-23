package com.finditnow.auth.types;

public enum Role {
    CUSTOMER, ADMIN, SHOP, DELIVERY_AGENT, UNASSIGNED;

    public static Role fromDb(String value) {
        return Role.valueOf(value.toUpperCase());
    }

    public String toDb() {
        return name().toLowerCase();
    }
}
