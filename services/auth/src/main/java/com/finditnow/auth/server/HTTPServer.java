package com.finditnow.auth.server;

import com.finditnow.auth.controller.AuthController;
import com.finditnow.auth.controller.OauthController;
import com.finditnow.auth.handlers.JwtAuthHandler;
import com.finditnow.auth.handlers.RequestLoggingHandler;
import com.finditnow.auth.handlers.Routes;
import com.finditnow.config.Config;
import com.finditnow.jwt.JwtService;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class HTTPServer {
    private static final Logger logger = LoggerFactory.getLogger(HTTPServer.class);

    public HTTPServer(AuthController authController, OauthController oauthController, JwtService jwtService) {
        RoutingHandler routes = Routes.build(authController, oauthController);

        Set<String> privateRoutes = Set.of(
                "/updatepassword",
                "/updaterole",
                "/logout"
        );

        JwtAuthHandler jwtAuthHandler = new JwtAuthHandler(routes, jwtService, privateRoutes);

        HttpHandler root = new RequestLoggingHandler(jwtAuthHandler);

        int httpPort = Integer.parseInt(Config.get("AUTH_SERVICE_PORT", "8080"));

        Undertow server = Undertow.builder().addHttpListener(httpPort, Config.get("AUTH_BIND_HOST", "0.0.0.0")).setHandler(exchange -> {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

            try {
                root.handleRequest(exchange);
            } catch (Exception ex) {
                logger.error("Error occurred during {}, Message: {}", exchange.getRequestPath(), ex.getMessage());
                if (!exchange.isResponseStarted()) {
                    exchange.setStatusCode(500);
                    exchange.getResponseSender().send("{\"error\": \"Internal Server Error\"}");
                }
            }
        }).build();

        server.start();

        logger.info("Auth Server started on port: {}", httpPort);
    }
}

