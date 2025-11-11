package com.finditnow.auth.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import com.finditnow.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.undertow.server.HttpServerExchange;

public class OAuthService {
    private final ObjectMapper mapper = new ObjectMapper();
    private final UserService userService;
    private final JwtService jwt;

    public OAuthService(UserService userService, JwtService jwt) {
        this.userService = userService;
        this.jwt = jwt;
    }

    public void handleGoogle(HttpServerExchange exchange) throws Exception {
        String body = new String(exchange.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, Object> req = mapper.readValue(body, Map.class);
        String idToken = (String) req.get("id_token");
        if (idToken == null) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"missing_id_token\"}");
            return;
        }

        Map<String, Object> payload = decodeJwtPayload(idToken);
        if (payload == null || payload.get("email") == null) {
            exchange.setStatusCode(401);
            exchange.getResponseSender().send("{\"error\":\"invalid_id_token\"}");
            return;
        }

        String email = (String) payload.get("email");

        String userId = userService.findOrCreateUserByEmail(email);

        String profile = req.getOrDefault("auth_profile", "customer").toString();

        String refreshToken = userService.createSession(userId, "google", profile, 7L * 24 * 60 * 60 * 1000L);

        String access = jwt.generateAccessToken(userId, profile);

        exchange.getResponseSender()
                .send("{\"access_token\":\"" + access + "\",\"refresh_token\":\"" + refreshToken + "\"}");
    }

    private Map<String, Object> decodeJwtPayload(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return mapper.readValue(payloadJson, Map.class);
        } catch (Exception e) {
            return null;
        }
    }
}

