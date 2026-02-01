package com.finditnow.auth.server;

import com.finditnow.auth.controller.AuthController;
import com.finditnow.auth.controller.OauthController;
import com.finditnow.auth.controller.ServiceTokenController;
import com.finditnow.auth.handlers.CorsHandler;
import com.finditnow.auth.handlers.JwtAuthHandler;
import com.finditnow.auth.handlers.RequestLoggingHandler;
import com.finditnow.auth.handlers.Routes;
import com.finditnow.auth.swagger.OpenApiController;
import com.finditnow.auth.swagger.SwaggerController;
import com.finditnow.config.Config;
import com.finditnow.jwt.JwtService;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.PathHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class HTTPServer {

    private static final Logger logger = LoggerFactory.getLogger(HTTPServer.class);

    public HTTPServer(
            AuthController authController,
            OauthController oauthController,
            ServiceTokenController serviceTokenController,
            JwtService jwtService
    ) {

        RoutingHandler routes =
                Routes.build(authController, oauthController, serviceTokenController);

        Set<String> privateRoutes = Set.of(
                "/updatepassword",
                "/updaterole",
                "/logout"
        );

        JwtAuthHandler jwtAuthHandler =
                new JwtAuthHandler(routes, jwtService, privateRoutes);

        PathHandler pathHandler = new PathHandler()

                // ---------- SWAGGER ----------
                .addExactPath("/auth/swagger", SwaggerController::ui)
                .addExactPath("/auth/openapi.json", OpenApiController::json)

                // ---------- AUTH ROUTES ----------
                .addPrefixPath(
                        "/auth",
                        new RequestLoggingHandler(jwtAuthHandler)
                );

        // 🔥 CORS MUST WRAP EVERYTHING
        CorsHandler corsRoot = new CorsHandler(pathHandler);

        int httpPort = Integer.parseInt(
                Config.get("AUTH_SERVICE_PORT", "8080")
        );

        Undertow server = Undertow.builder()
                .addHttpListener(httpPort, "localhost")
                .setHandler(corsRoot)
                .build();

        server.start();
        logger.info("Auth Server started on port: {}", httpPort);
    }
}
