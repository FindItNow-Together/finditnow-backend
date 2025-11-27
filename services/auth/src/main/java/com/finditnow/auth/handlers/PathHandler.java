package com.finditnow.auth.handlers;

import com.finditnow.auth.utils.Logger;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;

import java.util.HashMap;
import java.util.Map;

public class PathHandler {
    private static final Logger logger = Logger.getLogger(PathHandler.class);
    private static final Map<String, String> routeMap = new HashMap<>();
    public static final AttachmentKey<Long> REQUEST_START_TIME_KEY = AttachmentKey.create(Long.class);

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

    private final AuthHandler nextHandler;

    public PathHandler(AuthHandler nextHandler) {
        this.nextHandler = nextHandler;
    }

    public void verifyPath(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            // Track request start time early
            long startTime = System.nanoTime();
            exchange.putAttachment(REQUEST_START_TIME_KEY, startTime);

            exchange.dispatch(() -> {
                try {
                    verifyPath(exchange);
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


        nextHandler.route(exchange);
    }
}
