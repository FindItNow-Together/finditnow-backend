package com.finditnow.auth.handlers;

import com.finditnow.auth.controller.AuthController;
import com.finditnow.auth.controller.OauthController;
import com.finditnow.auth.controller.ServiceTokenController;
import io.undertow.server.RoutingHandler;

/**
 * this class is used for method and path based route handling
 */
public final class Routes {

    /**
     *
     * @param auth  controller for auth based routes
     * @param oauth controller for oauth based routes
     * @return RoutingHandler from undertow which supports method and path matching
     */
    public static RoutingHandler build(
            AuthController auth,
            OauthController oauth,
            ServiceTokenController serviceTokenController) {
        return new RoutingHandler()
                .post("/signin", auth::signIn)
                .post("/signup", auth::signUp)
                .post("/verifyemail", auth::verifyEmail)
                .post("/resendverificationemail", auth::resendVerificationEmail)
                .post("/refresh", auth::refresh)
                .post("/logout", auth::logout)
                .post("/sendresettoken", auth::sendResetPwdToken)
                .get("/verifyresettoken", auth::verifyResetToken)
                .put("/resetpassword", auth::resetPassword)
                .put("/updatepassword", auth::updatePassword)
                .put("/updaterole", auth::updateRole)

                .get("/oauth/google", oauth::redirectToGoogle)
                .get("/oauth/google/callback", oauth::authorizeGoogleResponse)

                // -------- internal --------
                .post("/internal/service-token", serviceTokenController::handle)

                .get("/health", exchange -> {
                    exchange.setStatusCode(200);
                    exchange.getResponseSender().send("{\"status\":\"ok\"}");
                });
    }
}

