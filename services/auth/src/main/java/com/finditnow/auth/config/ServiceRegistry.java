package com.finditnow.auth.config;

import com.finditnow.config.Config;

import java.util.Map;
import java.util.Set;

public final class ServiceRegistry {
    // service → secret
    private static final Map<String, String> SERVICE_SECRETS = Map.of(
            "order-service", Config.get("ORDER_SERVICE_SECRET", "RANDOM_SECRET_1"),
            "delivery-service", Config.get("DELIVERY_SERVICE_SECRET", "RANDOM_SECRET_2"),
            "shop-service", Config.get("SHOP_SERVICE_SECRET", "RANDOM_SECRET_3")
    );

    // service → allowed targets
    private static final Map<String, Set<String>> CALL_GRAPH = Map.of(
            "order-service", Set.of("delivery-service", "shop-service", "user-service"),
            "delivery-service", Set.of("order-service", "shop-service", "user-service"),
            "shop-service", Set.of("delivery-service", "order-service", "user-service")
    );

    public boolean authenticate(String service, String secret) {
        return secret.equals(SERVICE_SECRETS.get(service));
    }

    public boolean canCall(String from, String to) {
        return CALL_GRAPH
                .getOrDefault(from, Set.of())
                .contains(to);
    }
}
