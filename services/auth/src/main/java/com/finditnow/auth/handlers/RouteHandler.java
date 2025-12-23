package com.finditnow.auth.handlers;

import com.finditnow.auth.controller.AuthController;
import com.finditnow.auth.controller.OauthController;
import com.finditnow.auth.utils.Logger;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class RouteHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(RouteHandler.class);
    private final AuthController authController;
    private final OauthController oauthController;

    public RouteHandler(AuthController authController, OauthController oauthController) {
        this.authController = authController;
        this.oauthController = oauthController;
    }

    public final void handleRequest(HttpServerExchange exchange) throws Exception {
        String route = exchange.getRequestPath();

        switch (route) {
            case "/signin":
                authController.signIn(exchange);
                break;
            case "/signup":
                authController.signUp(exchange);
                break;
            case "/verifyemail":
                authController.verifyEmail(exchange);
                break;
            case "/resendverificationemail":
                authController.resendVerificationEmail(exchange);
                break;
            case "/oauth/google":
                oauthController.redirectToGoogle(exchange);

            case "/oauth/google/callback":
                oauthController.authorizeGoogleResponse(exchange);
//            case "/oauth/google/signin":
//                oauthController.handleGoogle(exchange);
//                break;
            case "/refresh":
                authController.refresh(exchange);
                break;
            case "/logout":
                authController.logout(exchange);
                break;

            case "/sendresettoken":
                authController.sendResetPwdToken(exchange);
                break;
            case "/verifyresettoken":
                authController.verifyResetToken(exchange);
                break;
            case "/resetpassword":
                authController.resetPassword(exchange);
                break;
            case "/updatepassword":
                authController.updatePassword(exchange);
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
