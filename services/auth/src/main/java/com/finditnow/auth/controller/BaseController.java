package com.finditnow.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finditnow.auth.dto.AuthResponse;
import com.finditnow.config.Config;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.CookieImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class BaseController {
    protected final ObjectMapper mapper = new ObjectMapper();
    protected final long refreshTokenMaxLifeSeconds = Duration.ofDays(7).toSeconds();

    public Map<String, String> getRequestBody(HttpServerExchange exchange) throws IOException {
        String body = new String(exchange.getInputStream().readAllBytes(), UTF_8);
        return mapper.readValue(body, Map.class);
    }

    protected void sendCommonResponse(HttpServerExchange exchange, AuthResponse resp) throws IOException {
        exchange.setStatusCode(resp.statusCode());
        exchange.getResponseSender().send(mapper.writeValueAsString(resp.data()));
    }

    protected void sendResponse(HttpServerExchange exchange, int statusCode, Map<String, String> data) throws IOException {
        exchange.setStatusCode(statusCode);
        exchange.getResponseSender().send(mapper.writeValueAsString(data));
    }

    protected void sendError(HttpServerExchange exchange, int statusCode, String error) throws IOException {
        exchange.setStatusCode(statusCode);
        exchange.getResponseSender().send("{\"error\":\"" + error + "\"}");
    }

    protected void setRefreshCookie(HttpServerExchange exchange, String token, boolean isExpired) {
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

    public <T> T getRequestBody(HttpServerExchange exchange, Class<T> clazz) throws IOException {
        String body = new String(exchange.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return mapper.readValue(body, clazz);
    }
}
