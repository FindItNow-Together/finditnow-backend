package com.finditnow.redis;

import com.finditnow.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Map;

public class RedisStore {
    private static final Logger logger = LoggerFactory.getLogger(RedisStore.class);
    private static RedisStore store;
    private final JedisPool pool;

    private RedisStore() {
        String redisHost = Config.get("REDIS_HOST", "localhost");
        int redisPort = Integer.parseInt(Config.get("REDIS_PORT", "6379"));

        this.pool = new JedisPool(redisHost, redisPort);
        ;
    }

    public static RedisStore getInstance() {
        if (store != null)
            return store;

        store = new RedisStore();

        try (Jedis j = store.pool.getResource()) {
            logger.info("Redis connection successful");
        } catch (Exception e) {
            logger.error("Failed to connect to Redis", e);
            throw new RuntimeException("Failed to connect to Redis", e);
        }
        return store;
    }

    public void putRefreshToken(String refreshToken, String sessionId, String profile, long ttlMillis) {
        System.out.println("TTLMillis: " + ttlMillis);
        try (Jedis j = pool.getResource()) {
            String key = "refresh:" + refreshToken;
            String val = sessionId + "|" + profile;
            j.setex(key, ttlMillis / 1000L, val);
        }
    }

    public Map<String, String> getRefreshToken(String refreshToken) {
        try (Jedis j = pool.getResource()) {
            String key = "refresh:" + refreshToken;
            String val = j.get(key);
            if (val == null)
                return null;
            String[] parts = val.split("\\|", 2);
            Map<String, String> m = new HashMap<>();
            m.put("userId", parts[0]);
            m.put("profile", parts[1]);
            return m;
        }
    }

    public void deleteRefreshToken(String refreshToken) {
        try (Jedis j = pool.getResource()) {
            String key = "refresh:" + refreshToken;
            j.del(key);
            logger.debug("Deleted refresh token: {}", refreshToken);
        }
    }

    public void blacklistAccessToken(String accessToken, long ttlSeconds) {
        try (Jedis j = pool.getResource()) {
            // Store the token directly as the key with a marker value
            String key = "blacklist:access:" + accessToken;
            // Store a marker (using "1" as marker)
            j.setex(key, ttlSeconds, "1");
        }
    }

    public boolean isAccessTokenBlacklisted(String accessToken) {
        try (Jedis j = pool.getResource()) {
            String key = "blacklist:access:" + accessToken;
            return j.exists(key);
        }
    }

    /**
     * @param ttlSeconds expiration time in seconds for the key value
     */
    public void setKey(String key, String value, long ttlSeconds) {
        try (Jedis jed = pool.getResource()) {
            jed.setex(key + ":", ttlSeconds, value);
        }
    }

    public String getKeyValue(String key) {
        try (Jedis jed = pool.getResource()) {
            return jed.get(key + ":");
        }
    }
}
