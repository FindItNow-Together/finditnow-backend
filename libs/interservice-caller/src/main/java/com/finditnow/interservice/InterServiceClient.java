package com.finditnow.interservice;

import com.finditnow.config.Config;
import com.finditnow.redis.RedisStore;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InterServiceClient {
    private static String thisService;
    private static String serviceSecret;
    private static final String authServiceUrl = resolveServiceUrl("auth-service");

    private static final HttpClient http = HttpClient.newHttpClient();
    private static final Map<String, CachedToken> tokenCache = new ConcurrentHashMap<>();
    private static RedisStore redisStore;
    private static final long DEFAULT_CACHE_TTL_SECONDS = 300;

    // ---------- BOOTSTRAP ----------

    public static void init(String serviceName, String secret) {
        thisService = serviceName;
        serviceSecret = secret;

        // Initialize Redis for response caching
        try {
            redisStore = RedisStore.getInstance();
        } catch (Exception e) {
            // Log warning but continue - caching will be disabled
            System.err.println("Warning: Redis not available, response caching disabled");
        }
    }

    // ---------- PUBLIC API ----------

    public static HttpResponse<String> call(String toService, String path, String method, String body) throws Exception {
        return call(toService, path, method, body, false, DEFAULT_CACHE_TTL_SECONDS);
    }

    /**
     * Overloaded method with cache flag
     * @param toService target service name
     * @param path API path
     * @param method HTTP method
     * @param body request body (can be null for GET)
     * @param useCache whether to use Redis cache (only applies to GET requests)
     * @return HTTP response
     */
    public static HttpResponse<String> call(String toService, String path, String method, String body, boolean useCache) throws Exception {
        return call(toService, path, method, body, useCache, DEFAULT_CACHE_TTL_SECONDS);
    }

    /**
     * Overloaded method with cache flag and custom TTL
     * @param toService target service name
     * @param path API path
     * @param method HTTP method
     * @param body request body (can be null for GET)
     * @param useCache whether to use Redis cache (only applies to GET requests)
     * @param cacheTtlSeconds cache TTL in seconds
     * @return HTTP response
     */
    public static HttpResponse<String> call(String toService, String path, String method, String body,
                                            boolean useCache, long cacheTtlSeconds) throws Exception {

        // Only cache GET requests
        if (useCache && "GET".equalsIgnoreCase(method) && redisStore != null) {
            return callWithCache(toService, path, method, body, cacheTtlSeconds);
        }

        // Fall back to original behavior for non-GET or when caching is disabled
        return callWithoutCache(toService, path, method, body);
    }

    // ---------- CACHE-ENABLED CALL ----------

    private static HttpResponse<String> callWithCache(String toService, String path, String method,
                                                      String body, long cacheTtlSeconds) throws Exception {
        String cacheKey = buildCacheKey(toService, path, body);

        // Try to get from cache
        String cachedResponse = redisStore.getKeyValue(cacheKey);
        if (cachedResponse != null) {
            return deserializeCachedResponse(cachedResponse);
        }

        // Cache miss - make the actual call
        HttpResponse<String> response = callWithoutCache(toService, path, method, body);

        // Cache successful responses (2xx status codes)
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            String serialized = serializeResponse(response);
            redisStore.setKey(cacheKey, serialized, cacheTtlSeconds);
        }

        return response;
    }

    private static HttpResponse<String> callWithoutCache(String toService, String path, String method,
                                                         String body) throws Exception {
        String token = getServiceToken(toService);

        HttpRequest request = buildRequest(toService, path, method, body, token);

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        // Retry ONCE if token expired or rejected downstream
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            tokenCache.remove(toService);

            token = getServiceToken(toService);
            request = buildRequest(toService, path, method, body, token);
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        }

        return response;
    }

    // ---------- CACHE UTILITIES ----------

    private static String buildCacheKey(String toService, String path, String body) {
        // Create a unique cache key based on service, path, and body
        String keyBase = "api-cache:" + toService + ":" + path;

        if (body != null && !body.isEmpty()) {
            // Include hash of body for requests with query parameters in body
            int bodyHash = body.hashCode();
            keyBase += ":" + bodyHash;
        }

        return keyBase;
    }

    private static String serializeResponse(HttpResponse<String> response) {
        // Simple serialization: statusCode|body
        return response.statusCode() + "|" + response.body();
    }

    private static HttpResponse<String> deserializeCachedResponse(String cached) {
        String[] parts = cached.split("\\|", 2);
        int statusCode = Integer.parseInt(parts[0]);
        String body = parts.length > 1 ? parts[1] : "";

        // Create a mock HttpResponse with cached data
        return new CachedHttpResponse(statusCode, body);
    }

    /**
     * Invalidate cache for a specific service and path
     */
    public static void invalidateCache(String toService, String path) {
        if (redisStore != null) {
            String cacheKey = buildCacheKey(toService, path, null);
            redisStore.deleteKey(cacheKey);
        }
    }

    // ---------- TOKEN HANDLING ----------

    private static String getServiceToken(String audience) throws Exception {
        CachedToken cached = tokenCache.get(audience);

        if (cached != null && cached.isValidSoon()) {
            return cached.token;
        }

        synchronized (InterServiceClient.class) {
            cached = tokenCache.get(audience);
            if (cached != null && cached.isValidSoon()) {
                return cached.token;
            }

            CachedToken fresh = fetchTokenFromAuthService(audience);
            tokenCache.put(audience, fresh);
            return fresh.token;
        }
    }

    private static CachedToken fetchTokenFromAuthService(String audience) throws Exception {

        String basicAuth = Base64.getEncoder().encodeToString((thisService + ":" + serviceSecret).getBytes());

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(authServiceUrl + "/internal/service-token")).header("Authorization", "Basic " + basicAuth).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{ \"audience\": \"" + audience + "\" }")).build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("Auth service rejected service token request: " + response.body());
        }

        Map<String, Object> json;

        try {
            json = JsonUtil.fromJson(response.body(), Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON payload", e);
        }

        String token = (String) json.get("accessToken");
        int expiresIn = (int) json.get("expiresIn");

        return new CachedToken(token, Instant.now().plusSeconds(expiresIn).toEpochMilli());
    }

    // ---------- REQUEST BUILDING ----------

    private static HttpRequest buildRequest(String toService, String path, String method, String body, String token) {
        String url = resolveServiceUrl(toService) + path;

        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url)).header("Authorization", "Bearer " + token).header("Content-Type", "application/json");

        return switch (method) {
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body)).build();
            case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(body)).build();
            case "DELETE" -> builder.DELETE().build();
            default -> builder.GET().build();
        };
    }

    public static String resolveServiceUrl(String toService) {
        String envKey = toService.toUpperCase().replace("-", "_");

        String host = Config.get(envKey + "_HOST", "http://localhost");
        String port = Config.get(envKey + "_PORT");

        if (port == null || port.isEmpty()) {
            throw new IllegalStateException("Missing env var: " + envKey);
        }

        return host + ":" + port;
    }

    // ---------- CACHE ----------

    private record CachedToken(String token, long expiresAtMillis) {

        boolean isValidSoon() {
            return System.currentTimeMillis() < expiresAtMillis - 15_000;
        }
    }

    // Simple implementation of HttpResponse for cached responses
    private record CachedHttpResponse(int statusCode, String body) implements HttpResponse<String> {

        @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return HttpHeaders.of(new HashMap<>(), (a, b) -> true);
            }

            @Override
            public Optional<SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return null;
            }

            @Override
            public HttpClient.Version version() {
                return HttpClient.Version.HTTP_1_1;
            }
        }
}


