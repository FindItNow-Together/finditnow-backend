package com.finditnow.auth;

import com.finditnow.auth.config.DatabaseMigrations;
import com.finditnow.auth.config.GrpcHealthChecker;
import com.finditnow.auth.controller.AuthController;
import com.finditnow.auth.controller.OauthController;
import com.finditnow.auth.controller.ServiceTokenController;
import com.finditnow.auth.dao.AuthDao;
import com.finditnow.auth.jobs.RevokeExpiredSessionJob;
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

    // Singleton holder pattern - thread-safe lazy initialization
    private static class ServiceHolder {
        private static volatile boolean initialized = false;
        private static AuthService authService;
        private static OAuthService oauthService;
        private static JwtService jwtService;
        private static RedisStore redisStore;
        private static AuthDao authDao;
    }

    public static void main(String[] args) {
        try {
            String grpcHost = Config.get("USER_SERVICE_GRPC_HOST", "localhost");
            int grpcPort = Integer.parseInt(Config.get("USER_SERVICE_GRPC_PORT", "8082"));

            GrpcHealthChecker.waitForGrpcServer(grpcHost, grpcPort, 10, 2000);

            DataSource ds = new Database("auth_service").get();
            DatabaseMigrations.migrate(ds);

            // Initialize services
            ServiceHolder.redisStore = RedisStore.getInstance();
            ServiceHolder.jwtService = new JwtService();
            ServiceHolder.authDao = new AuthDao(ds);
            ServiceHolder.authService = new AuthService(
                    ServiceHolder.authDao,
                    ServiceHolder.redisStore,
                    ServiceHolder.jwtService
            );
            ServiceHolder.oauthService = new OAuthService(
                    ServiceHolder.authService,
                    ServiceHolder.redisStore,
                    ServiceHolder.jwtService
            );
            ServiceHolder.initialized = true;

            AuthController authController = new AuthController(ServiceHolder.authService);
            OauthController oauthController = new OauthController(ServiceHolder.oauthService);

            Scheduler.init();
            RevokeExpiredSessionJob.runJob();

            new HTTPServer(
                    authController,
                    oauthController,
                    new ServiceTokenController(ServiceHolder.jwtService),
                    ServiceHolder.jwtService
            );

            logger.info("AuthApp started successfully");

        } catch (Exception e) {
            logger.error("Failed to start server", e);
            logger.error("SERVER DID NOT START! TERMINATING...");
            System.exit(1);
        }
    }

    // Public accessor methods with initialization check
    public static AuthService getAuthService() {
        ensureInitialized();
        return ServiceHolder.authService;
    }

    public static OAuthService getOAuthService() {
        ensureInitialized();
        return ServiceHolder.oauthService;
    }

    public static JwtService getJwtService() {
        ensureInitialized();
        return ServiceHolder.jwtService;
    }

    public static RedisStore getRedisStore() {
        ensureInitialized();
        return ServiceHolder.redisStore;
    }

    public static AuthDao getAuthDao() {
        ensureInitialized();
        return ServiceHolder.authDao;
    }

    private static void ensureInitialized() {
        if (!ServiceHolder.initialized) {
            throw new IllegalStateException("AuthApp services not initialized. Ensure main() has been called.");
        }
    }
}
