package com.finditnow.auth;

import com.finditnow.auth.config.DatabaseMigrations;
import com.finditnow.auth.config.GrpcHealthChecker;
import com.finditnow.auth.controller.AuthController;
import com.finditnow.auth.controller.OauthController;
import com.finditnow.auth.dao.AuthDao;
import com.finditnow.auth.server.HTTPServer;
import com.finditnow.auth.service.AuthService;
import com.finditnow.auth.service.OAuthService;
import com.finditnow.config.Config;
import com.finditnow.database.Database;
import com.finditnow.jwt.JwtService;
import com.finditnow.redis.RedisStore;
import com.finditnow.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public class AuthApp {
    private static final Logger logger = LoggerFactory.getLogger(AuthApp.class);

    public static void main(String[] args) {

        try {
            String grpcHost = Config.get("USER_SERVICE_GRPC_HOST", "localhost");
            int grpcPort = Integer.parseInt(Config.get("USER_SERVICE_GRPC_PORT", "8083"));

            GrpcHealthChecker.waitForGrpcServer(grpcHost, grpcPort, 10, 2000);

            DataSource ds = new Database("auth_service").get();
            DatabaseMigrations.migrate(ds);
            RedisStore redis = RedisStore.getInstance();
            JwtService jwt = new JwtService();
            AuthDao authDao = new AuthDao(ds);
            AuthService authService = new AuthService(authDao, redis, jwt);
            AuthController authController = new AuthController(authService);

            OAuthService oauth = new OAuthService(authService, redis, jwt);
            OauthController oauthController = new OauthController(oauth);
            Scheduler.init();
            new HTTPServer(authController, oauthController, jwt);
        } catch (Exception e) {
            logger.error("Failed to start server", e);
            logger.error("SERVER DID NOT START! TERMINATING...");
            System.exit(1);
        }
    }
}
