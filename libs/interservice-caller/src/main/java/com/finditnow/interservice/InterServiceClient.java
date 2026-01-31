package com.finditnow.interservice;

import com.finditnow.config.Config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InterServiceClient {
    private static String thisService;
    private static String serviceSecret;
    private static final String authServiceUrl = resolveServiceUrl("auth-service");

    private static final HttpClient http = HttpClient.newHttpClient();
    private static final Map<String, CachedToken> tokenCache = new ConcurrentHashMap<>();

    // ---------- BOOTSTRAP ----------

    public static void init(String serviceName, String secret) {
        thisService = serviceName;
        serviceSecret = secret;
    }

    // ---------- PUBLIC API ----------

    public static HttpResponse<String> call(String toService, String path, String method, String body) throws Exception {

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
}
