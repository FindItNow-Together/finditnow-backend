package com.finditnow.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finditnow.auth.dto.AuthResponse;
import com.finditnow.auth.service.OAuthService;
import com.finditnow.config.Config;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.CookieImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public class OauthController {
    private final ObjectMapper mapper = new ObjectMapper();
    private final OAuthService oAuthService;
    private final long refreshTokenMaxLifeSeconds = Duration.ofDays(7).toSeconds();

    public OauthController(OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    public void handleGoogle(HttpServerExchange exchange) throws Exception {
        Map<String, Object> req = getRequestBody(exchange);
        String idToken = (String) req.get("id_token");

        if (idToken == null) {
            sendError(exchange, 400, "missing_id_token");
            return;
        }

        String authProfile = req.getOrDefault("auth_profile", "customer").toString();

        try {
            AuthResponse response = oAuthService.handleGoogleAuth(idToken, authProfile);

            if (response.isSuccess() && response.getData().containsKey("refresh_token")) {
                setRefreshCookie(exchange, response.getData().get("refresh_token"), false);
                response.getData().remove("refresh_token");
            }

            sendResponse(exchange, response.getStatusCode(), response.getData());
        } catch (Exception e) {
            sendError(exchange, 500, "internal_server_error");
        }
    }

    // Helper methods for HTTP concerns
    private Map<String, Object> getRequestBody(HttpServerExchange exchange) throws IOException {
        String body = new String(exchange.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return mapper.readValue(body, Map.class);
    }

    private void sendResponse(HttpServerExchange exchange, int statusCode, Map<String, String> data) throws IOException {
        exchange.setStatusCode(statusCode);
        exchange.getResponseSender().send(mapper.writeValueAsString(data));
    }

    private void sendError(HttpServerExchange exchange, int statusCode, String error) throws IOException {
        exchange.setStatusCode(statusCode);
        exchange.getResponseSender().send("{\"error\":\"" + error + "\"}");
    }

    private void setRefreshCookie(HttpServerExchange exchange, String token, boolean isExpired) {
        CookieImpl cookie = new CookieImpl("refresh_token", token);
        cookie.setHttpOnly(true);

        if (isExpired) {
            cookie.setMaxAge(0);
        } else {
            cookie.setMaxAge((int) refreshTokenMaxLifeSeconds);
        }

        if (Config.get("ENVIRONMENT", "development").equals("development")) {
            cookie.setSameSiteMode("Lax");
            cookie.setPath("/");
        } else {
            cookie.setSameSiteMode("None");
            cookie.setPath("/");
            cookie.setSecure(true);
        }

        exchange.setResponseCookie(cookie);
    }
}
