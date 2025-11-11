package com.finditnow.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.finditnow.auth.db.UserDao;
import com.finditnow.auth.server.HTTPServer;
import com.finditnow.auth.service.OAuthService;
import com.finditnow.auth.service.UserService;
import com.finditnow.database.Database;
import com.finditnow.jwt.JwtService;
import com.finditnow.redis.RedisStore;

public class AuthApp {
    private static final Logger logger = LoggerFactory.getLogger(AuthApp.class);

    public static void main(String[] args) {
        try {
            Database db = new Database();
            RedisStore redis = RedisStore.getInstance();
            JwtService jwt = JwtService.getInstance();
            UserDao userDao = new UserDao(db);
            UserService usrService = new UserService(userDao, redis, jwt);
            OAuthService oauth = new OAuthService(usrService, jwt);

            new HTTPServer(usrService, oauth).start();
        } catch (Exception e) {
            logger.error("Failed to start server", e);
            logger.error("SERVER DID NOT START! TERMINATING...");
            System.exit(1);
        }
    }
}

