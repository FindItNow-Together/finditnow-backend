package com.reviewsystem.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * JWT Configuration that automatically loads settings from:
 * 1. Environment variables (JWT_SECRET, JWT_EXPIRATION)
 * 2. System properties
 * 3. External auth configuration file
 * 4. Existing application session configuration
 */
@Configuration
@Getter
public class JwtConfig {

    private final Environment environment;

    @Value("${jwt.secret:#{null}}")
    private String jwtSecret;

    @Value("${jwt.expiration:900000}")
    private long jwtExpiration;

    // Path to your existing auth configuration file (if any)
    @Value("${auth.config.path:#{null}}")
    private String authConfigPath;

    public JwtConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        // Priority 1: Check if JWT_SECRET is already set from environment
        if (jwtSecret != null && !jwtSecret.isEmpty()) {
            System.out.println("✓ JWT Secret loaded from environment/properties");
            return;
        }

        // Priority 2: Try to load from external auth configuration file
        if (authConfigPath != null && !authConfigPath.isEmpty()) {
            loadFromAuthConfig();
        }

        // Priority 3: Try to load from common auth config locations
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            tryLoadFromCommonLocations();
        }

        // Validation: Ensure JWT secret is set
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException(
                    "JWT Secret not configured! Please set one of:\n" +
                            "1. Environment variable: JWT_SECRET\n" +
                            "2. System property: jwt.secret\n" +
                            "3. External auth config file path: auth.config.path\n" +
                            "4. Or set directly in application.yml");
        }

        System.out.println("✓ JWT Configuration loaded successfully");
        System.out.println("  - Secret length: " + jwtSecret.length() + " characters");
        System.out.println("  - Expiration: " + (jwtExpiration / 60000) + " minutes");
    }

    /**
     * Load JWT configuration from external auth config file
     */
    private void loadFromAuthConfig() {
        try {
            Properties authProps = new Properties();
            authProps.load(new FileInputStream(authConfigPath));

            // Try different common property names
            String secret = authProps.getProperty("jwt.secret");
            if (secret == null)
                secret = authProps.getProperty("JWT_SECRET");
            if (secret == null)
                secret = authProps.getProperty("security.jwt.secret");
            if (secret == null)
                secret = authProps.getProperty("app.jwt.secret");

            if (secret != null && !secret.isEmpty()) {
                this.jwtSecret = secret;
                System.out.println("✓ JWT Secret loaded from auth config: " + authConfigPath);
            }

            // Load expiration if available
            String expiration = authProps.getProperty("jwt.expiration");
            if (expiration == null)
                expiration = authProps.getProperty("JWT_EXPIRATION");
            if (expiration != null) {
                this.jwtExpiration = Long.parseLong(expiration);
            }
        } catch (IOException e) {
            System.err.println("⚠ Could not load auth config from: " + authConfigPath);
        }
    }

    /**
     * Try to load from common configuration file locations
     */
    private void tryLoadFromCommonLocations() {
        String[] commonPaths = {
                "config/auth.properties",
                "config/application.properties",
                "../config/auth.properties",
                System.getProperty("user.home") + "/.config/app/auth.properties"
        };

        for (String path : commonPaths) {
            try {
                Properties props = new Properties();
                props.load(new FileInputStream(path));

                String secret = props.getProperty("jwt.secret");
                if (secret == null)
                    secret = props.getProperty("JWT_SECRET");

                if (secret != null && !secret.isEmpty()) {
                    this.jwtSecret = secret;
                    System.out.println("✓ JWT Secret loaded from: " + path);

                    String expiration = props.getProperty("jwt.expiration");
                    if (expiration != null) {
                        this.jwtExpiration = Long.parseLong(expiration);
                    }
                    return;
                }
            } catch (IOException e) {
                // Silently continue to next location
            }
        }
    }

    /**
     * Get JWT secret (for use in your JWT utility class)
     */
    public String getSecret() {
        return jwtSecret;
    }

    /**
     * Get JWT expiration time in milliseconds
     */
    public long getExpiration() {
        return jwtExpiration;
    }

    /**
     * Get JWT expiration time in seconds
     */
    public long getExpirationInSeconds() {
        return jwtExpiration / 1000;
    }
}
