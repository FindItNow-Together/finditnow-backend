package com.finditnow.auth.handlers;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finditnow.auth.service.OAuthService;
import com.finditnow.auth.service.AuthService;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;

public class AuthHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuthHandler.class);
    private static final AttachmentKey<Long> REQUEST_START_TIME_KEY = AttachmentKey.create(Long.class);
    private final AuthService authService;
    private final OAuthService oauthService;

    public AuthHandler(AuthService usrService, OAuthService oauth) {
        authService = usrService;
        oauthService = oauth;
    }

    public final void route(HttpServerExchange exchange)
            throws Exception {

        if (exchange.isInIoThread()) {
            // Track request start time early
            long startTime = System.nanoTime();
            exchange.putAttachment(REQUEST_START_TIME_KEY, startTime);

            exchange.dispatch(() -> {
                try {
                    route(exchange);
                } catch (Exception e) {
                    Long start = exchange.getAttachment(REQUEST_START_TIME_KEY);
                    long executionTimeMs = 0;
                    if (start != null) {
                        executionTimeMs = (System.nanoTime() - start) / 1_000_000;
                    }
                    logger.error("Exception handling request {} {} - {}ms",
                            exchange.getRequestMethod(), exchange.getRequestPath(), executionTimeMs, e);
                    exchange.setStatusCode(500);
                    exchange.getResponseSender().send("{\"error\":\"internal_error\"}");
                }
            });
            return;
        }

        String method = exchange.getRequestMethod().toString();
        String path = exchange.getRequestPath();

        // Track request start time
        long startTime = System.nanoTime();
        exchange.putAttachment(REQUEST_START_TIME_KEY, startTime);

        // Log incoming request
        logger.info("Incoming request: {} {}", method, path);

        Map<String, String> routeMap = new HashMap<>();
        routeMap.put("/signin", "POST");
        routeMap.put("/signup", "POST");
        routeMap.put("/verifyemail", "POST");
        routeMap.put("/oauth/google/signin", "POST");
        routeMap.put("/refresh", "POST");
        routeMap.put("/logout", "POST");
        routeMap.put("/health", "GET");

        String route = exchange.getRequestPath();
        exchange.startBlocking();

        if (!routeMap.containsKey(route)) {
            exchange.setStatusCode(404);
            exchange.getResponseSender().send("{\"error\":\"invalid request\"}");
            logResponse(exchange);
            return;
        }

        String expectedMethod = routeMap.get(route);

        if (!expectedMethod.equalsIgnoreCase(exchange.getRequestMethod().toString())) {
            exchange.setStatusCode(405);
            exchange.getResponseSender().send("{\"error\":\"invalid request\"}");
            logResponse(exchange);
            return;
        }

        switch (route) {
            case "/signin":
                authService.signIn(exchange);
                break;
            case "/signup":
                authService.signUp(exchange);
                break;
            case "/verifyemail":
                authService.verifyEmail(exchange);
                break;
            case "/oauth/google/signin":
                oauthService.handleGoogle(exchange);
                break;
            case "/refresh":
                authService.refresh(exchange);
                break;
            case "/logout":
                authService.logout(exchange);
                break;
            case "/health":
                exchange.setStatusCode(200);
                exchange.getResponseSender().send("{\"status\":\"ok\"}");
                break;
            default:
                exchange.setStatusCode(404);
                exchange.getResponseSender().send("{\"error\":\"invalid request path\"}");
                break;
        }

        logResponse(exchange);
    }

    private void logResponse(HttpServerExchange exchange) {
        int statusCode = exchange.getStatusCode();
        String method = exchange.getRequestMethod().toString();
        String path = exchange.getRequestPath();

        // Calculate execution time
        Long startTime = exchange.getAttachment(REQUEST_START_TIME_KEY);
        long executionTimeMs = 0;
        if (startTime != null) {
            executionTimeMs = (System.nanoTime() - startTime) / 1_000_000; // Convert nanoseconds to milliseconds
        }

        if (statusCode >= 200 && statusCode < 300) {
            logger.info("Response: {} {} {} (SUCCESS) - {}ms", method, path, statusCode, executionTimeMs);
        } else if (statusCode >= 400 && statusCode < 500) {
            logger.warn("Response: {} {} {} (CLIENT_ERROR) - {}ms", method, path, statusCode, executionTimeMs);
        } else if (statusCode >= 500) {
            logger.error("Response: {} {} {} (SERVER_ERROR) - {}ms", method, path, statusCode, executionTimeMs);
        } else {
            logger.info("Response: {} {} {} - {}ms", method, path, statusCode, executionTimeMs);
        }
    }
}
