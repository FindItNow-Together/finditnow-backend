package com.finditnow.auth.handlers;

import com.finditnow.auth.service.AuthService;
import com.finditnow.auth.service.OAuthService;
import com.finditnow.auth.utils.Logger;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;

public class AuthHandler {
    private static final Logger logger = Logger.getLogger(AuthHandler.class);
    private static final AttachmentKey<Long> REQUEST_START_TIME_KEY = AttachmentKey.create(Long.class);
    private final AuthService authService;
    private final OAuthService oauthService;

    public AuthHandler(AuthService usrService, OAuthService oauth) {
        authService = usrService;
        oauthService = oauth;
    }

    public final void route(HttpServerExchange exchange) throws Exception {
        String route = exchange.getRequestPath();

        switch (route) {
            case "/signin":
                authService.signIn(exchange);
                break;
            case "/signup":
                authService.signUp(exchange);
                break;
            case "/verifyemail":
                authService.verifyEmail(exchange);
                break;
            case "/resendverificationemail":
                authService.resendVerificationEmail(exchange);
                break;
            case "/oauth/google/signin":
                oauthService.handleGoogle(exchange);
                break;
            case "/refresh":
                authService.refresh(exchange);
                break;
            case "/logout":
                authService.logout(exchange);
                break;

            case "/sendresettoken":
                authService.sendResetPwdToken(exchange);
                break;
            case "/verifyresettoken":
                authService.verifyResetToken(exchange);
                break;
            case "/resetpassword":
                authService.resetPassword(exchange);
                break;
            case "/updatepassword":
                authService.updatePassword(exchange);
                break;
            case "/health":
                exchange.setStatusCode(200);
                exchange.getResponseSender().send("{\"status\":\"ok\"}");
                break;
            default:
                exchange.setStatusCode(404);
                exchange.getResponseSender().send("{\"error\":\"invalid request path\"}");
                break;
        }

        logger.logResponse(exchange);
    }
}
