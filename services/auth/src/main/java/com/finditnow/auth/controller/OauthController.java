package com.finditnow.auth.controller;

import com.finditnow.auth.dto.AuthResponse;
import com.finditnow.auth.dto.GoogleTokenResponse;
import com.finditnow.auth.service.OAuthService;
import com.finditnow.config.Config;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

public class OauthController extends BaseController {
    private final OAuthService oAuthService;
    private final long refreshTokenMaxLifeSeconds = Duration.ofDays(7).toSeconds();

    public OauthController(OAuthService oAuthService) {
        super();
        this.oAuthService = oAuthService;
    }

    public void redirectToGoogle(HttpServerExchange exchange) throws Exception {
        if (oAuthService.OAUTH_CLIENT_ID == null || oAuthService.OAUTH_CLIENT_SECRET == null) {
            exchange.setStatusCode(StatusCodes.UNAUTHORIZED);
            exchange.getResponseSender().send("{\"error\":\"auth credentials not found\"}");
            return;
        }

        String state = UUID.randomUUID().toString();
        oAuthService.saveOauthState(state);

        String authUrl =
                "https://accounts.google.com/o/oauth2/v2/auth" +
                        "?client_id=" + url(oAuthService.OAUTH_CLIENT_ID) +
                        "&redirect_uri=" + url(oAuthService.OAUTH_REDIRECT_URI) +
                        "&response_type=code" +
                        "&scope=" + url("openid email profile") +
                        "&state=" + url(state);

        exchange.setStatusCode(StatusCodes.FOUND);
        exchange.getResponseHeaders().put(Headers.LOCATION, authUrl);
        exchange.endExchange();
    }

    public void authorizeGoogleResponse(HttpServerExchange exchange) throws Exception {
        String code = exchange.getQueryParameters().get("code") != null
                ? exchange.getQueryParameters().get("code").getFirst()
                : null;

        String state = exchange.getQueryParameters().get("state") != null
                ? exchange.getQueryParameters().get("state").getFirst()
                : null;

        if (code == null || state == null) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("{\"error\":\"authorization failed due to lack of credentials\"}");
            return;
        }

        if (!oAuthService.existsOauthState(state)) {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("{\"error\":\"authorization failed; invalid request\"}");
            return;
        }

        if (exchange.getQueryParameters().containsKey("error")) {
            exchange.setStatusCode(StatusCodes.UNAUTHORIZED);
            exchange.getResponseSender().send(
                    "{\"error\":\"google authorization denied\"}"
            );
            return;
        }

        try (HttpClient client = HttpClient.newHttpClient();) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(getFormBody(code)))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                exchange.setStatusCode(StatusCodes.CONFLICT);
                exchange.getResponseSender().send(
                        "{\"error\":\"google authorization failed\"}"
                );
                return;
            }

            GoogleTokenResponse googleTokenResponse = mapper.readValue(response.body(), GoogleTokenResponse.class);

            AuthResponse resp = oAuthService.handleGoogleAuth(googleTokenResponse.getIdToken());

            if (resp.isSuccess() && resp.getData().containsKey("refresh_token")) {
                setRefreshCookie(exchange, resp.getData().get("refresh_token"), false);
                resp.getData().remove("refresh_token");
            }

            if (!resp.isSuccess()) {
                sendError(exchange, resp.getStatusCode(), resp.getData().get("error"));
                return;
            }

            exchange.setStatusCode(StatusCodes.FOUND);
            exchange.getResponseHeaders().put(Headers.LOCATION, Config.get("FRONTEND_APP_OAUTH_CALLBACK_URL"));
            exchange.endExchange();
        }
    }

    private String getFormBody(String code) {
        return "client_id=" + URLEncoder.encode(oAuthService.OAUTH_CLIENT_ID, UTF_8) +
                "&client_secret=" + URLEncoder.encode(oAuthService.OAUTH_CLIENT_SECRET, UTF_8) +
                "&code=" + URLEncoder.encode(code, UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(oAuthService.OAUTH_REDIRECT_URI, UTF_8) +
                "&grant_type=authorization_code";
    }

//    public void handleGoogle(HttpServerExchange exchange) throws Exception {
//        Map<String, String> req = getRequestBody(exchange);
//        String idToken = req.get("id_token");
//
//        if (idToken == null) {
//            sendError(exchange, 400, "missing_id_token");
//            return;
//        }
//
//        String authProfile = req.getOrDefault("auth_profile", "customer").toString();
//
//        try {
//            AuthResponse response = oAuthService.handleGoogleAuth(idToken, authProfile);
//
//            if (response.isSuccess() && response.getData().containsKey("refresh_token")) {
//                setRefreshCookie(exchange, response.getData().get("refresh_token"), false);
//                response.getData().remove("refresh_token");
//            }
//
//            sendResponse(exchange, response.getStatusCode(), response.getData());
//        } catch (Exception e) {
//            sendError(exchange, 500, "internal_server_error");
//        }
//    }

    private String url(String value) {
        return URLEncoder.encode(value, UTF_8);
    }
}
