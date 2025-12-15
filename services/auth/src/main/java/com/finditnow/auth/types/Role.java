package com.finditnow.auth.types;

public enum Role {
    CUSTOMER, ADMIN, SHOP, DELIVERY;

    public String toDb() {
        return name().toLowerCase();
    }

    public static Role fromDb(String value) {
        return Role.valueOf(value.toUpperCase());
    }
}
