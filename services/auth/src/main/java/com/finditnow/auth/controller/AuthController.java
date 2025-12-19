package com.finditnow.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finditnow.auth.dto.AuthResponse;
import com.finditnow.auth.dto.SignUpDto;
import com.finditnow.auth.handlers.PathHandler;
import com.finditnow.auth.service.AuthService;
import com.finditnow.auth.utils.Logger;
import com.finditnow.config.Config;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public class AuthController {
    private static final Logger logger = Logger.getLogger(AuthController.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final AuthService authService;
    private final long refreshTokenMaxLifeSeconds = Duration.ofDays(7).toSeconds();

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public void signUp(HttpServerExchange exchange) throws Exception {
        SignUpDto signUpReq = getRequestBody(exchange, SignUpDto.class);

        if (!signUpReq.isValid()) {
            sendError(exchange, 400, "missing_fields");
            return;
        }

        if (signUpReq.getRole() == null) {
            signUpReq.setRole("customer");
        }

        try {
            AuthResponse response = authService.signUp(signUpReq);
            sendResponse(exchange, response.statusCode(), response.data());
        } catch (Exception e) {
            logger.getCore().error("Signup failed", e);
            sendError(exchange, 500, "internal_server_error");
        }
    }

    public void verifyEmail(HttpServerExchange exchange) throws Exception {
        Map<String, String> bodyMap = getRequestBody(exchange);
        String credId = bodyMap.get("credId");
        String verificationOtp = bodyMap.get("verificationCode");

        if (credId == null || verificationOtp == null) {
            sendError(exchange, 400, "missing_fields");
            return;
        }

        AuthResponse response = authService.verifyEmail(credId, verificationOtp);

        if (response.isSuccess() && response.data().containsKey("sessionToken")) {
            setRefreshCookie(exchange, response.data().get("sessionToken"), false);
            response.data().remove("sessionToken");
        }

        sendResponse(exchange, response.statusCode(), response.data());
    }

    public void resendVerificationEmail(HttpServerExchange exchange) throws Exception {
        Map<String, String> bodyMap = getRequestBody(exchange);
        String credId = bodyMap.get("credId");

        if (credId == null || credId.isEmpty()) {
            sendError(exchange, 400, "missing_fields");
            return;
        }

        AuthResponse response = authService.resendVerificationEmail(credId);
        sendResponse(exchange, response.statusCode(), response.data());
    }

    public void sendResetPwdToken(HttpServerExchange exchange) throws Exception {
        Map<String, String> bodyMap = getRequestBody(exchange);
        String email = bodyMap.get("email");
        String phone = bodyMap.get("phone");

        if (email == null && phone == null) {
            sendError(exchange, 400, "missing_fields");
            return;
        }

        AuthResponse response = authService.sendResetPwdToken(email, phone);
        sendResponse(exchange, response.statusCode(), response.data());
    }

    public void verifyResetToken(HttpServerExchange exchange) throws Exception {
        Map<String, String> bodyMap = getRequestBody(exchange);
        String email = bodyMap.get("email");
        String phone = bodyMap.get("phone");
        String token = bodyMap.get("resetToken");

        if (email == null && phone == null) {
            sendError(exchange, 400, "missing_fields");
            return;
        }
        if (token == null) {
            sendError(exchange, 400, "missing_token");
            return;
        }

        AuthResponse response = authService.verifyResetToken(email, phone, token);
        sendResponse(exchange, response.statusCode(), response.data());
    }

    public void resetPassword(HttpServerExchange exchange) throws Exception {
        Map<String, String> bodyMap = getRequestBody(exchange);
        String email = bodyMap.get("email");
        String phone = bodyMap.get("phone");
        String password = bodyMap.get("newPassword");

        if (email == null && phone == null) {
            sendError(exchange, 400, "either phone or email is required");
            return;
        }

        if (password == null || password.isEmpty()) {
            sendError(exchange, 400, "password is required");
            return;
        }

        AuthResponse response = authService.resetPassword(email, phone, password);
        sendResponse(exchange, response.statusCode(), response.data());
    }

    public void updatePassword(HttpServerExchange exchange) throws Exception {
        Map<String, String> bodyMap = getRequestBody(exchange);
        String newPassword = bodyMap.get("newPassword");

        if (newPassword == null) {
            sendError(exchange, 400, "password_required");
            return;
        }

        Map<String, String> authInfo = exchange.getAttachment(PathHandler.SESSION_INFO);
        String credId = authInfo.get("credId");

        AuthResponse response = authService.updatePassword(credId, newPassword);
        sendResponse(exchange, response.statusCode(), response.data());
    }

    public void signIn(HttpServerExchange exchange) throws Exception {
        Map<String, String> req = getRequestBody(exchange);
        String identifier = req.get("email");
        String password = req.get("password");

        if (identifier == null || password == null) {
            sendError(exchange, 400, "missing_fields");
            return;
        }

        AuthResponse response = authService.signIn(identifier, password);

        if (response.isSuccess() && response.data().containsKey("sessionToken")) {
            setRefreshCookie(exchange, response.data().get("sessionToken"), false);
            response.data().remove("sessionToken");
        }

        sendResponse(exchange, response.statusCode(), response.data());
    }

    public void logout(HttpServerExchange exchange) throws Exception {
        String accessToken = exchange.getAttachment(PathHandler.AUTH_TOKEN);
        String refreshToken = null;

        Cookie refreshCookie = exchange.getRequestCookie("refresh_token");
        if (refreshCookie != null) {
            refreshToken = refreshCookie.getValue();
        }

        if (accessToken == null && refreshToken == null) {
            sendResponse(exchange, 200, Map.of("message", "already logged out"));
            return;
        }

        AuthResponse response = authService.logout(accessToken, refreshToken);
        setRefreshCookie(exchange, "", true);
        sendResponse(exchange, response.statusCode(), response.data());
    }

    public void refresh(HttpServerExchange exchange) throws Exception {
        Cookie refreshCookie = exchange.getRequestCookie("refresh_token");

        if (refreshCookie == null) {
            sendError(exchange, 400, "missing_refresh_token");
            return;
        }

        String refreshToken = refreshCookie.getValue();
        AuthResponse response = authService.refresh(refreshToken);
        sendResponse(exchange, response.statusCode(), response.data());
    }

    // Helper methods for HTTP concerns
    private Map<String, String> getRequestBody(HttpServerExchange exchange) throws IOException {
        String body = new String(exchange.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return mapper.readValue(body, Map.class);
    }

    private <T> T getRequestBody(HttpServerExchange exchange, Class<T> clazz) throws IOException {
        String body = new String(exchange.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return mapper.readValue(body, clazz);
    }

    private void sendResponse(HttpServerExchange exchange, int statusCode, Map<String, String> data) throws IOException {
        exchange.setStatusCode(statusCode);
        exchange.getResponseSender().send(mapper.writeValueAsString(data));
    }

    private void sendError(HttpServerExchange exchange, int statusCode, String error) throws IOException {
        exchange.setStatusCode(statusCode);
        exchange.getResponseSender().send("{\"error\":\"" + error + "\"}");
    }

    private void setRefreshCookie(HttpServerExchange exchange, String token, boolean isExpired) {
        CookieImpl cookie = new CookieImpl("refresh_token", token);
        cookie.setHttpOnly(true);

        if (isExpired) {
            cookie.setMaxAge(0);
        } else {
            cookie.setMaxAge((int) refreshTokenMaxLifeSeconds);
        }

        if (Config.get("ENVIRONMENT", "development").equals("development")) {
            cookie.setSameSiteMode("Lax");
            cookie.setPath("/");
        } else {
            cookie.setSameSiteMode("None");
            cookie.setPath("/refresh");
            cookie.setSecure(true);
        }

        exchange.setResponseCookie(cookie);
    }
}
