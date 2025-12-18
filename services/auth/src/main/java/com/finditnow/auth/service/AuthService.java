package com.finditnow.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finditnow.auth.dao.AuthDao;
import com.finditnow.auth.handlers.PathHandler;
import com.finditnow.auth.model.AuthCredential;
import com.finditnow.auth.model.AuthSession;
import com.finditnow.auth.utils.Logger;
import com.finditnow.common.OtpGenerator;
import com.finditnow.common.PasswordUtil;
import com.finditnow.config.Config;
import com.finditnow.dispatcher.EmailDispatcher;
import com.finditnow.jwt.JwtService;
import com.finditnow.mail.MailService;
import com.finditnow.redis.RedisStore;
import com.finditnow.user.CreateUserProfileRequest;
import com.finditnow.user.UserServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AuthService {
    private static final Logger logger = Logger.getLogger(AuthService.class);
    private static final EmailDispatcher mailer = new EmailDispatcher(new MailService());
    private final ObjectMapper mapper = new ObjectMapper();
    private final AuthDao authDao;
    private final RedisStore redis;
    private final JwtService jwt;

    private final long refreshTokenMaxLifeSeconds = Duration.ofDays(7).toSeconds();

    public AuthService(AuthDao authDao, RedisStore redis, JwtService jwt) {
        this.authDao = authDao;
        this.redis = redis;
        this.jwt = jwt;
    }

    // signup with email + password
    public void signUp(HttpServerExchange exchange) throws Exception {
        Map<String, String> req = getRequestBody(exchange);
        String email = req.get("email");
        String firstName = req.get("firstName");
        String phone = req.get("phone");
        String password = req.get("password");
        String role = req.get("role");

        if ((email == null && phone == null) || password == null || firstName == null) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"missing_fields\"}");
            return;
        }

        if (role == null) {
            role = "customer";
        }

        Optional<AuthCredential> authCred = authDao.credDao.findByEmail(email);

        if (email != null && authCred.isPresent()) {
            exchange.setStatusCode(409);
            if (authCred.get().isEmailVerified()) {
                exchange.getResponseSender().send("{\"error\":\"user_verified\"}");
            } else {
                Map<String, String> res = new HashMap<>();
                res.put("error", "account_not_verified");
                res.put("credId", authCred.get().getId().toString());
                exchange.setStatusCode(400);
                exchange.getResponseSender().send(mapper.writeValueAsString(res));
            }

            return;
        }

        UUID credId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // if(!PasswordUtil.checkPwdString(password)) {
        // exchange.setStatusCode(409);
        // exchange.getResponseSender().send("{\"error\":\"password not in desired
        // format\"}");
        // return;
        // }

        String pwHash = PasswordUtil.hash(password);

        AuthCredential cred = new AuthCredential(credId, userId, email, phone, pwHash, role, false, false,
                OffsetDateTime.now());
        cred.setFirstName(firstName);
        authDao.credDao.insert(cred);

        String emailOtp = sendVerificationEmail(email, credId.toString());

        Map<String, String> resp = new HashMap<>();

        resp.put("credId", credId.toString());
        resp.put("message", "verification email sent");

        resp.put("accessTokenValiditySeconds", "120");

        if (Config.get("ENVIRONMENT", "development").equals("development")) {
            resp.put("emailOtp", emailOtp);
        }

        exchange.setStatusCode(201);
        exchange.getResponseSender().send(mapper.writeValueAsString(resp));
    }

    public void verifyEmail(HttpServerExchange exchange) throws Exception {
        Map<String, String> bodyMap = getRequestBody(exchange);

        String credId = bodyMap.get("credId");
        String verificationOtp = bodyMap.get("verificationCode");

        if (credId == null || verificationOtp == null) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"missing_fields\"}");
            return;
        }

        String storedOtp = redis.getKeyValue("emailOtp:" + credId);

        if (storedOtp == null) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"otp_not_found\"}");
            return;
        }

        if (!storedOtp.equals(verificationOtp)) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"invalid_otp\"}");
            return;
        }

        Map<String, Object> updateFields = new HashMap<>();

        updateFields.put("is_email_verified", true);
        authDao.credDao.updateCredFieldsById(UUID.fromString(credId), updateFields);

        Optional<AuthCredential> authCred = authDao.credDao.findById(UUID.fromString(credId));
        AuthCredential cred = authCred.get();

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(Config.get("USER_SERVICE_GRPC_HOST", "localhost"),
                        Integer.parseInt(Config.get("USER_SERVICE_GRPC_PORT", "8083")))
                .usePlaintext().build();

        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);

        var res = stub.createUserProfile(CreateUserProfileRequest.newBuilder().setId(cred.getUserId().toString())
                .setEmail(cred.getEmail()).setName(cred.getFirstName()).build());

        if (!res.hasUser()) {
            exchange.setStatusCode(500);
            exchange.getResponseSender().send("{\"error\":\"internal server error\"}");
            return;
        }

        AuthSession authSession = createSessionFromCred(cred);

        String accessToken = jwt.generateAccessToken(authSession.getId().toString(), cred.getId().toString(),
                cred.getUserId().toString(), cred.getRole().toString());

        addSessionToRedis(authSession, cred.getUserId().toString(), cred.getRole().toString());

        Map<String, String> resp = new HashMap<>();
        resp.put("accessToken", accessToken);
        resp.put("profile", cred.getRole().toString());
        resp.put("firstName", cred.getFirstName());

        setRefreshCookie(exchange, authSession.getSessionToken(), false);
        exchange.getResponseSender().send(mapper.writeValueAsString(resp));
    }

    public void resendVerificationEmail(HttpServerExchange exchange) throws Exception {
        Map<String, String> bodyMap = getRequestBody(exchange);

        String credId = bodyMap.get("credId");

        // email is required for sending verification email
        if (credId == null || credId.isEmpty()) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"missing_fields\"}");
            return;
        }

        Optional<AuthCredential> authCred = authDao.credDao.findById(UUID.fromString(credId));

        if (authCred.isEmpty()) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"email_not_registered\"}");
            return;
        }

        AuthCredential cred = authCred.get();

        if (cred.isEmailVerified()) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"email_verified\"}");
            return;
        }

        String email = cred.getEmail();

        String emailOtp = sendVerificationEmail(email, credId);

        Map<String, String> resp = new HashMap<>();

        resp.put("message", "verification email sent");
        resp.put("accessTokenValiditySeconds", "120");

        if (Config.get("ENVIRONMENT", "development").equals("development")) {
            resp.put("emailOtp", emailOtp);
        }
        exchange.setStatusCode(201);
        exchange.getResponseSender().send(mapper.writeValueAsString(resp));
    }

    private String sendVerificationEmail(String email, String credId) {
        String emailOtp = OtpGenerator.generateSecureOtp(6);

        mailer.send(email, "FindItNow: Email Verification", String.format(
                "Your email verification code: <strong style=\"font-size:18px\">%s</strong>.<br><p style=\"font-weight:700\">This email is system generated. Do not reply</p>",
                emailOtp), true);

        redis.setKey("emailOtp:" + credId, emailOtp, 2 * 60L);
        return emailOtp;
    }

    public void sendResetPwdToken(HttpServerExchange exchange) throws Exception {
        Map<String, String> bodyMap = getRequestBody(exchange);
        String email = bodyMap.get("email");
        String phone = bodyMap.get("phone");

        if (email == null && phone == null) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"missing_fields\"}");
            return;
        }

        // if (email!=null &&
        // !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
        // exchange.setStatusCode(400);
        // exchange.getResponseSender().send("{\"error\":\"invalid email\"}");
        // return;
        // }

        Optional<AuthCredential> authCred = authDao.credDao.findByEmail(email);

        if (authCred.isEmpty()) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"email not registered\"}");
            return;
        }

        String resetOtp = OtpGenerator.generateSecureOtp(8);
        mailer.send(email, "Reset Password Token:Finditnow", String.format(
                "Your reset password token: <strong style=\\\"font-size:18px\\\">%s</strong>.<br><p style=\\\"font-weight:700\\\">"
                        + "This email is system generated. Do not reply</p>",
                resetOtp), true);
        redis.setKey("resetOtp:" + email, resetOtp, Duration.ofMinutes(5).toSeconds());

        exchange.setStatusCode(200);
        exchange.getResponseSender()
                .send("{\"message\":\"verification email resent\", \"tokenValiditySeconds\": \"300\"}");
    }

    public void verifyResetToken(HttpServerExchange exchange) throws Exception {
        Map<String, String> bodyMap = getRequestBody(exchange);
        String email = bodyMap.get("email");
        String phone = bodyMap.get("phone");
        String token = bodyMap.get("resetToken");

        if (email == null && phone == null) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"missing_fields\"}");
            return;
        }
        if (token == null) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"missing_token\"}");
            return;
        }

        // if (email!=null &&
        // !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
        // exchange.setStatusCode(400);
        // exchange.getResponseSender().send("{\"error\":\"invalid email\"}");
        // return;
        // }

        String storedToken = redis.getKeyValue("resetOtp:" + email);
        if (storedToken == null) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"invalid_token\"}");
            return;
        }

        if (!storedToken.equals(token)) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"invalid_token\"}");
            return;
        }

        redis.deleteKey("resetOtp:" + email);
        redis.setKey("resetAllowed:" + email, String.valueOf(true), Duration.ofMinutes(2).toSeconds());
        exchange.setStatusCode(200);
        exchange.getResponseSender().send("{\"message\":\"token_verified\"}");
    }

    public void resetPassword(HttpServerExchange exchange) throws Exception {
        Map<String, String> bodyMap = getRequestBody(exchange);

        String email = bodyMap.get("email");
        String phone = bodyMap.get("phone");
        String password = bodyMap.get("newPassword");

        if (email == null && phone == null) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"either phone or email is required\"}");
            return;
        }

        if (password == null || password.isEmpty()) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"password is required\"}");
            return;
        }

        // if(!PasswordUtil.checkPwdString(password)) {
        // exchange.setStatusCode(409);
        // exchange.getResponseSender().send("{\"error\":\"password not in desired
        // format\"}");
        // return;
        // }

        if (redis.getKeyValue("resetAllowed:" + email) != null) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"reset window exceeded\"}");
            return;
        }

        AuthCredential cred = authDao.credDao.findByEmail(email).get();

        Map<String, Object> updateFields = new HashMap<>();

        updateFields.put("password_hash", PasswordUtil.hash(password));
        authDao.credDao.updateCredFieldsById(cred.getId(), updateFields);

        redis.deleteKey("resetAllowed:" + email);
        exchange.setStatusCode(200);
        exchange.getResponseSender().send("{\"message\":\"password updated\"}");
    }

    public void updatePassword(HttpServerExchange exchange) throws Exception {
        Map<String, String> bodyMap = getRequestBody(exchange);

        String newPassword;
        if ((newPassword = bodyMap.get("newPassword")) == null) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"password_required\"}");
            return;
        }

        if (!PasswordUtil.checkPwdString(newPassword)) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"password not in desired format\"}");
            return;
        }

        Map<String, String> authInfo = exchange.getAttachment(PathHandler.SESSION_INFO);

        Map<String, Object> updateFields = new HashMap<>();
        updateFields.put("password_hash", newPassword);
        authDao.credDao.updateCredFieldsById(UUID.fromString(authInfo.get("credId")), updateFields);
    }

    // signin using email or phone or username + password
    public void signIn(HttpServerExchange exchange) throws Exception {
        Map<String, String> req = getRequestBody(exchange);
        String identifier = req.get("email"); // email or phone or username
        String password = req.get("password");

        if (identifier == null || password == null) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"missing_fields\"}");
            return;
        }

        Optional<AuthCredential> authCred = authDao.credDao.findByIdentifier(identifier);
        if (authCred.isEmpty()) {
            exchange.setStatusCode(401);
            exchange.getResponseSender().send("{\"error\":\"invalid_credentials\"}");
            return;
        }

        AuthCredential cred = authCred.get();

        if (!cred.isEmailVerified() && !cred.isPhoneVerified()) {
            Map<String, String> res = new HashMap<>();
            res.put("error", "account_not_verified");
            res.put("credId", cred.getId().toString());
            exchange.setStatusCode(400);
            exchange.getResponseSender().send(mapper.writeValueAsString(res));
            return;
        }

        if (!PasswordUtil.verifyPassword(password, cred.getPasswordHash())) {
            exchange.setStatusCode(401);
            exchange.getResponseSender().send("{\"error\":\"invalid_credentials\"}");
            return;
        }

        AuthSession authSession = createSessionFromCred(cred);

        String accessToken = jwt.generateAccessToken(authSession.getId().toString(), cred.getId().toString(),
                cred.getUserId().toString(), cred.getRole().toString());
        addSessionToRedis(authSession, cred.getUserId().toString(), cred.getRole().toString());

        Map<String, String> resp = new HashMap<>();

        resp.put("accessToken", accessToken);
        resp.put("profile", cred.getRole().toString());
        resp.put("firstName", cred.getFirstName());

        setRefreshCookie(exchange, authSession.getSessionToken(), false);

        exchange.getResponseSender().send(mapper.writeValueAsString(resp));
    }

    private Map<String, String> getRequestBody(HttpServerExchange exchange) throws IOException {
        String body = new String(exchange.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> req = mapper.readValue(body, Map.class);
        return req;
    }

    public void logout(HttpServerExchange exchange) throws Exception {
        String accessToken = exchange.getAttachment(PathHandler.AUTH_TOKEN);
        Map<String, String> tokenUserInfo = exchange.getAttachment(PathHandler.SESSION_INFO);
        String refreshToken = null;
        if (accessToken == null) {
            Cookie refreshCookie = exchange.getRequestCookie("refresh_token");

            if (refreshCookie == null) {
                exchange.setStatusCode(200);
                exchange.getResponseSender().send("{\"message\":\"already logged out\"}");
                return;
            }

            refreshToken = refreshCookie.getValue();

            authDao.sessionDao.invalidateByToken(refreshToken);
        } else {
            refreshToken = authDao.sessionDao.invalidate(UUID.fromString(tokenUserInfo.get("sessionId")));
        }

        // Delete refresh token from DB and Redis

        redis.deleteRefreshToken(refreshToken);

        // Blacklist the access token if provided
        try {
            long ttlSeconds = jwt.getTokenRemainingTtlSeconds(accessToken);
            if (ttlSeconds > 0) {
                redis.blacklistAccessToken(accessToken, ttlSeconds);
            }
        } catch (Exception e) {
            // Log error but don't fail logout if token blacklisting fails
            logger.getCore().warn("Failed to blacklist access token during logout", e);
        }

        setRefreshCookie(exchange, "", true);
        exchange.getResponseSender().send("{\"message\":\"logged out successfully\"}");
    }

    private void setRefreshCookie(HttpServerExchange exchange, String token, boolean isExpired) throws IOException {
        CookieImpl cookie = new CookieImpl("refresh_token", token);
        cookie.setHttpOnly(true);

        if (isExpired) {
            cookie.setMaxAge(0);
        } else {
            cookie.setMaxAge(((int) refreshTokenMaxLifeSeconds));
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

    public AuthSession createSessionFromCred(AuthCredential cred) {
        AuthSession authSession = new AuthSession(UUID.randomUUID(), cred.getId(), UUID.randomUUID().toString(),
                "password", OffsetDateTime.now().plusSeconds(refreshTokenMaxLifeSeconds));

        authDao.sessionDao.insert(authSession);

        return authSession;
    }

    // create server session: stores session in DB and Redis
    void addSessionToRedis(AuthSession authSession, String userId, String role) throws Exception {
        // store in Redis: key refresh:<token> -> userId|profile
        redis.putRefreshToken(authSession.getSessionToken(), authSession.getId().toString(),
                authSession.getCredId().toString(), userId, role,
                Duration.between(OffsetDateTime.now(), authSession.getExpiresAt()).toMillis());
    }

    // refresh access token flow
    public void refresh(HttpServerExchange exchange) throws Exception {
        Cookie refreshCookie = exchange.getRequestCookie("refresh_token");

        if (refreshCookie == null) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"missing_refresh_token\"}");
            return;
        }

        String refreshToken = refreshCookie.getValue();

        // lookup redis
        Map<String, String> info = redis.getRefreshToken(refreshToken);

        if (info == null) {
            exchange.setStatusCode(401);
            exchange.getResponseSender().send("{\"error\":\"invalid_refresh\"}");
            return;
        }

        String newAccess = jwt.generateAccessToken(info.get("sessionId"), info.get("credId"), info.get("userId"),
                info.get("profile"));

        Map<String, String> resp = new HashMap<>();

        resp.put("accessToken", newAccess);
        resp.put("profile", info.get("profile"));
        exchange.getResponseSender().send(mapper.writeValueAsString(resp));
    }

    // used by OAuth service to find or create user by email
    public AuthCredential findOrCreateUserByEmail(String email) throws Exception {
        Optional<AuthCredential> u = authDao.credDao.findByEmail(email);

        if (u.isPresent()) {
            return u.get();
        }
        UUID id = UUID.randomUUID();
        AuthCredential cred = new AuthCredential();
        cred.setId(id);
        cred.setEmailVerified(true);
        cred.setEmail(email);
        authDao.credDao.insert(cred);
        return cred;
    }
}
