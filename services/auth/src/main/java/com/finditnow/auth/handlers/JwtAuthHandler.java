package com.finditnow.auth.handlers;

import com.finditnow.jwt.JwtService;
import com.finditnow.jwt.exceptions.JwtExpiredException;
import com.finditnow.jwt.exceptions.JwtInvalidException;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;

import java.util.Map;
import java.util.Set;

public class JwtAuthHandler implements HttpHandler {
    public static final AttachmentKey<String> AUTH_TOKEN = AttachmentKey.create(String.class);
    public static final AttachmentKey<Map<String, String>> SESSION_INFO = AttachmentKey.create(Map.class);

    private final HttpHandler next;
    private final JwtService jwt;
    private final Set<String> privateRoutes;

    public JwtAuthHandler(HttpHandler next, JwtService jwt, Set<String> privateRoutes) {
        this.next = next;
        this.jwt = jwt;
        this.privateRoutes = privateRoutes;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String path = exchange.getRequestPath();

        // Internal endpoints must bypass JWT logic entirely
        if (path.startsWith("/internal/")) {
            next.handleRequest(exchange);
            return;
        }

        boolean isPrivate = privateRoutes.contains(path);

        String authHeader =
                exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7);

            try {
                Map<String, String> sessionInfo =
                        jwt.parseTokenToUser(token);

                // keeping the auth info irrespective of private or public
                exchange.putAttachment(SESSION_INFO, sessionInfo);
                exchange.putAttachment(AUTH_TOKEN, token);

            } catch (JwtExpiredException e) {
                if (isPrivate) {
                    exchange.setStatusCode(401);
                    exchange.getResponseSender()
                            .send("{\"error\":\"token_expired\"}");
                    return;
                }
            } catch (JwtInvalidException e) {
                if (isPrivate) {
                    exchange.setStatusCode(401);
                    exchange.getResponseSender()
                            .send("{\"error\":\"unauthorized\"}");
                    return;
                }
            }

        } else if (isPrivate) {
            exchange.setStatusCode(401);
            exchange.getResponseSender()
                    .send("{\"error\":\"unauthorized\"}");
            return;
        }

        next.handleRequest(exchange);
    }
}
