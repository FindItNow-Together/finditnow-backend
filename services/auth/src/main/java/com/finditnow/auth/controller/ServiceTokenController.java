package com.finditnow.auth.controller;

import com.finditnow.auth.config.ServiceRegistry;
import com.finditnow.jwt.JwtService;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class ServiceTokenController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(ServiceTokenController.class);
    private final JwtService jwt;
    private static final ServiceRegistry registry = new ServiceRegistry();

    public ServiceTokenController(JwtService jwt) {
        this.jwt = jwt;
    }

    public void handle(HttpServerExchange exchange) throws Exception {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Basic ")) {
            exchange.setStatusCode(401);
            return;
        }

        log.info("Received auth header: {}", auth);

        String decoded = new String(
                Base64.getDecoder().decode(auth.substring(6)),
                StandardCharsets.UTF_8
        );
        log.info("decoded auth header: {}", decoded);
        String[] parts = decoded.split(":", 2);
        if (parts.length != 2 ||
                !registry.authenticate(parts[0], parts[1])) {
            exchange.setStatusCode(401);
            return;
        }

        String body = new String(exchange.getInputStream().readAllBytes());
        Map<String, String> reqBody = mapper.readValue(body, Map.class);

        if (!registry.canCall(parts[0], reqBody.get("audience"))) {
            exchange.setStatusCode(403);
            return;
        }

        String token = jwt.generateServiceToken(parts[0], reqBody.get("audience"));

        exchange.getResponseSender().send("""
                {
                  "accessToken": "%s",
                  "expiresIn": 60
                }
                """.formatted(token));
    }


}
