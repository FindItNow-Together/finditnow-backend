package com.finditnow.auth;

import com.finditnow.auth.config.DatabaseMigrations;
import com.finditnow.auth.dao.AuthDao;
import com.finditnow.auth.server.HTTPServer;
import com.finditnow.auth.service.AuthService;
import com.finditnow.auth.service.OAuthService;
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
            DataSource ds = new Database("auth_service").get();
            DatabaseMigrations.migrate(ds);
            RedisStore redis = RedisStore.getInstance();
            JwtService jwt = new JwtService();
            AuthDao authDao = new AuthDao(ds);
            AuthService authServ = new AuthService(authDao, redis, jwt);
            OAuthService oauth = new OAuthService(authServ, jwt);
            Scheduler.init();
            new HTTPServer(authServ, oauth).start();
        } catch (Exception e) {
            logger.error("Failed to start server", e);
            logger.error("SERVER DID NOT START! TERMINATING...");
            System.exit(1);
        }
    }
}

