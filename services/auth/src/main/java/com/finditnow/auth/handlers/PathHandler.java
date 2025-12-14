package com.finditnow.auth.handlers;

import com.finditnow.auth.utils.Logger;
import com.finditnow.jwt.JwtService;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PathHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(PathHandler.class);
    private static final Map<String, String> routeMap = new HashMap<>();
    private static final Set<String> privateRoutes = new HashSet<>();
    public static final AttachmentKey<Long> REQUEST_START_TIME_KEY = AttachmentKey.create(Long.class);
    public static final AttachmentKey<String> AUTH_TOKEN = AttachmentKey.create(String.class);
    public static final AttachmentKey<Map<String, String>> SESSION_INFO = AttachmentKey.create(Map.class);

    private static final JwtService jwt = new JwtService();

    static {
        routeMap.put("/signin", "POST");
        routeMap.put("/signup", "POST");
        routeMap.put("/verifyemail", "POST");
        routeMap.put("/resendverificationemail", "POST");
        routeMap.put("/oauth/google/signin", "POST");
        routeMap.put("/refresh", "POST");
        routeMap.put("/logout", "POST");
        routeMap.put("/sendresettoken", "POST");
        routeMap.put("/verifyresettoken", "GET");
        routeMap.put("/resetpassword", "PUT");
        routeMap.put("/updatepassword", "PUT");
        routeMap.put("/health", "GET");
    }

    static {
        privateRoutes.add("/updatepassword");
    }

    private final RouteHandler nextHandler;

    public PathHandler(RouteHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            // Track request start time early
            long startTime = System.nanoTime();
            exchange.putAttachment(REQUEST_START_TIME_KEY, startTime);

            exchange.dispatch(() -> {
                try {
                    handleRequest(exchange);
                } catch (Exception e) {
                    Long start = exchange.getAttachment(REQUEST_START_TIME_KEY);
                    long executionTimeMs = 0;
                    if (start != null) {
                        executionTimeMs = (System.nanoTime() - start) / 1_000_000;
                    }
                    logger.getCore().error("Exception handling request {} {} - {}ms", exchange.getRequestMethod(), exchange.getRequestPath(), executionTimeMs, e);
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
        logger.getCore().info("Incoming request: {} {}", method, path);

        String route = exchange.getRequestPath();
        exchange.startBlocking();

        if (!routeMap.containsKey(route)) {
            exchange.setStatusCode(404);
            exchange.getResponseSender().send("{\"error\":\"invalid request\"}");
            logger.logResponse(exchange);
            return;
        }


        String expectedMethod = routeMap.get(route);

        if (!expectedMethod.equalsIgnoreCase(exchange.getRequestMethod().toString())) {
            exchange.setStatusCode(405);
            exchange.getResponseSender().send("{\"error\":\"invalid request\"}");
            logger.logResponse(exchange);
            return;
        }

        String token = null;
        Map<String, String> sessionInfo = null;
        if (privateRoutes.contains(route)) {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                exchange.setStatusCode(401);
                exchange.getResponseSender().send("{\"error\":\"unauthorized\"}");
                return;
            }

            token = authHeader.substring(7);

            try {
                sessionInfo = jwt.parseTokenToUser(token);

                if (sessionInfo == null || sessionInfo.get("isSessionExpired").equals("true")) {
                    exchange.setStatusCode(401);
                    exchange.getResponseSender().send("{\"error\":\"token_expired\"}");
                    return;
                }


            } catch (Exception e) {
                exchange.setStatusCode(401);
                exchange.getResponseSender().send("{\"error\":\"unauthorized\"}");
                return;
            }


        }

        if (sessionInfo != null) {
            exchange.putAttachment(SESSION_INFO, sessionInfo);
            exchange.putAttachment(AUTH_TOKEN, token);
        }

        nextHandler.handleRequest(exchange);
    }
}
