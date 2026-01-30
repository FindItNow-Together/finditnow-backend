package com.finditnow.auth.controller;

import com.finditnow.auth.dto.AuthResponse;
import com.finditnow.auth.dto.SignUpDto;
import com.finditnow.auth.handlers.JwtAuthHandler;
import com.finditnow.auth.service.AuthService;
import com.finditnow.auth.types.Role;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

public class AuthController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        super();
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
            logger.error("Signup failed", e);
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

        Map<String, String> authInfo = exchange.getAttachment(JwtAuthHandler.SESSION_INFO);
        String credId = authInfo.get("credId");

        AuthResponse response = authService.updatePassword(credId, newPassword);
        sendResponse(exchange, response.statusCode(), response.data());
    }

    public void updateRole(HttpServerExchange exchange) throws Exception {
        Map<String, String> bodyMap = getRequestBody(exchange);

        String role = bodyMap.get("role");
        if (role == null || role.isEmpty()) {
            sendError(exchange, 400, "role_required");
            return;
        }

        Role roleCheck = Role.valueOf(role);

        Map<String, String> authInfo = exchange.getAttachment(JwtAuthHandler.SESSION_INFO);
        String credId = authInfo.get("credId");

        AuthResponse resp = authService.updateRoleByCredential(UUID.fromString(credId), authInfo.get("userId"), role);

        sendCommonResponse(exchange, resp);
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
        String accessToken = exchange.getAttachment(JwtAuthHandler.AUTH_TOKEN);
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
}
