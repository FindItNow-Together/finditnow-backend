package com.finditnow.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finditnow.auth.model.AuthCredential;
import com.finditnow.auth.model.AuthSession;
import com.finditnow.jwt.JwtService;
import io.undertow.server.HttpServerExchange;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class OAuthService {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AuthService authService;
    private final JwtService jwt;

    public OAuthService(AuthService authService, JwtService jwt) {
        this.authService = authService;
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

        AuthCredential cred = authService.findOrCreateUserByEmail(email);

        String profile = req.getOrDefault("auth_profile", cred.getRole().toString()).toString();

        AuthSession authSession = authService.createSessionFromCred(cred);

        String accessToken = jwt.generateAccessToken(authSession.getId().toString(), cred.getId().toString(),
                cred.getUserId().toString(), cred.getRole().toString());
        authService.addSessionToRedis(authSession, cred.getUserId().toString(), cred.getRole().toString());

        exchange.getResponseSender()
                .send("{\"access_token\":\"" + accessToken + "\",\"refresh_token\":\"" + authSession.getSessionToken()
                        + "\"}");
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
