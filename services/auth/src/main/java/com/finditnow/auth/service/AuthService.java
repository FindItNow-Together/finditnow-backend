package com.finditnow.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finditnow.auth.dao.AuthDao;
import com.finditnow.auth.model.AuthCredential;
import com.finditnow.auth.model.AuthSession;
import com.finditnow.common.OtpGenerator;
import com.finditnow.common.PasswordUtil;
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
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final EmailDispatcher mailer = new EmailDispatcher(new MailService());
    private final ObjectMapper mapper = new ObjectMapper();
    private final AuthDao authDao;
    private final RedisStore redis;
    private final JwtService jwt;

    public AuthService(AuthDao authDao, RedisStore redis, JwtService jwt) {
        this.authDao = authDao;
        this.redis = redis;
        this.jwt = jwt;
    }

    // signup with email + password
    public void signUp(HttpServerExchange exchange) throws Exception {
        String body = new String(exchange.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> req = mapper.readValue(body, Map.class);
        String email = req.get("email");
        String username = req.get("username");
        String phone = req.get("phone_no");
        String password = req.get("password");

        if ((email == null && phone == null && username == null) || password == null) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"missing_fields\"}");
            return;
        }

        if (email != null && authDao.credDao.findByEmail(email).isPresent()) {
            exchange.setStatusCode(409);
            exchange.getResponseSender().send("{\"error\":\"user_exists\"}");
            return;
        }

        UUID credId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String pwHash = PasswordUtil.hash(password);

        AuthCredential cred = new AuthCredential(credId, userId, email, phone, pwHash, false, false, OffsetDateTime.now());

        authDao.credDao.insert(cred);

        sendVerificationEmail(email, credId.toString());

        exchange.setStatusCode(201);
        exchange.getResponseSender().send("{\"message\":\"user_created\",\"credId\":\"" + credId + "\"}");
    }

    public void verifyEmail(HttpServerExchange exchange) throws Exception {
        String body = new String(exchange.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> bodyMap = mapper.readValue(body, Map.class);

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
            exchange.getResponseSender().send("{\"error\":\"otp not found\"}");
            return;
        }

        if (!storedOtp.equals(verificationOtp)) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"invalid otp\"}");
            return;
        }

        Map<String, Object> updateFields = new HashMap<>();

        updateFields.put("is_email_verified", true);
        authDao.credDao.updateCredFieldsById(credId, updateFields);

        Optional<AuthCredential> authCred = authDao.credDao.findById(UUID.fromString(credId));
        AuthCredential cred = authCred.get();

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8082).usePlaintext().build();

        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);

        var res = stub.createUserProfile(CreateUserProfileRequest.newBuilder().setId(cred.getUserId().toString()).setEmail("abc@Gmail.com").setName("pqr_123").build());

        System.out.println("GRPC RESPONSE -> " + res);

        if (!res.hasUser()) {
            exchange.setStatusCode(500);
            exchange.getResponseSender().send("{\"error\":\"internal server error\"}");
            return;
        }


        AuthSession authSession = createSessionFromCred(cred);

        String accessToken = jwt.generateAccessToken(authSession.getId().toString(), cred.getId().toString(), cred.getUserId().toString(), "customer");

        addSessionToRedis(authSession);

        Map<String, String> resp = new HashMap<>();
        resp.put("access_token", accessToken);
        exchange.getResponseHeaders().put(Headers.SET_COOKIE, "refresh_token=" + authSession.getSessionToken() + ";Secure; Path=/refresh; HttpOnly; SameSite=Strict");
        exchange.getResponseSender().send(mapper.writeValueAsString(resp));
    }

    public void resendVerificationEmail(HttpServerExchange exchange) throws Exception {
        String body = new String(exchange.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> bodyMap = mapper.readValue(body, Map.class);
        String email = bodyMap.get("email");

        //email is required for sending verification email
        if (email == null || email.isEmpty()) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"missing_fields\"}");
            return;
        }

        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"invalid email\"}");
            return;
        }

        Optional<AuthCredential> cred = authDao.credDao.findByEmail(email);

        if (cred.isEmpty()) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"email not registered\"}");
            return;
        }

        String credId = cred.get().getId().toString();

        sendVerificationEmail(email, credId);

        exchange.setStatusCode(201);
        exchange.getResponseSender().send("{\"message\":\"verification email resent\"");
    }

    private void sendVerificationEmail(String email, String credId) {
        String emailOtp = OtpGenerator.generateSecureOtp(6);

//        mailer.send(email, "FindItNow: Email Verification", String
//                .format("Your email verification code: <strong style=\"font-size:18px\">%s</strong>.<br><p style=\"font-weight:700\">This email is system generated. Do not reply</p>",
//                        emailOtp), true);

        redis.setKey("emailOtp:" + credId, String.valueOf(emailOtp), 2 * 60L);
    }

    // signin using email or phone or username + password
    public void signIn(HttpServerExchange exchange) throws Exception {
        String body = new String(exchange.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> req = mapper.readValue(body, Map.class);
        String identifier = req.get("identifier"); // email or phone or username
        String password = req.get("password");
        String authProfile = req.getOrDefault("auth_profile", "customer");

        if (identifier == null || password == null) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"missing_fields\"}");
            return;
        }

        Optional<AuthCredential> authCred = authDao.credDao.findByIdentifier(identifier);
        if (authCred.isEmpty() || !PasswordUtil.verifyPassword(password, authCred.get().getPasswordHash())) {
            exchange.setStatusCode(401);
            exchange.getResponseSender().send("{\"error\":\"invalid_credentials\"}");
            return;
        }

        AuthCredential cred = authCred.get();

        AuthSession authSession = createSessionFromCred(cred);

        String accessToken = jwt.generateAccessToken(authSession.getId().toString(), cred.getId().toString(), cred.getUserId().toString(), "customer");
        addSessionToRedis(authSession);

        Map<String, String> resp = new HashMap<>();

        resp.put("access_token", accessToken);
        exchange.getResponseHeaders().put(Headers.SET_COOKIE, "refresh_token=" + authSession.getSessionToken() + ";Secure; Path=/refresh; HttpOnly; SameSite=Strict");
        exchange.getResponseSender().send(mapper.writeValueAsString(resp));
    }

    public void logout(HttpServerExchange exchange) throws Exception {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null || authHeader.isEmpty()) {
            exchange.setStatusCode(401);
            exchange.getResponseSender().send("{\"error\":\"request unauthorized\"}");
            return;
        }

        String accessToken;

        try {
            accessToken = authHeader.substring(7);
        } catch (Exception e) {
            exchange.setStatusCode(401);
            exchange.getResponseSender().send("{\"error\":\"request unauthorized\"}");
            return;
        }

        Map<String, String> tokenUserInfo = jwt.parseTokenToUser(accessToken);

        // Delete refresh token from DB and Redis
        String dbRefreshToken = authDao.sessionDao.invalidate(UUID.fromString(tokenUserInfo.get("sessionId")));
        redis.deleteRefreshToken(dbRefreshToken);

        // Blacklist the access token if provided
        try {
            long ttlSeconds = jwt.getTokenRemainingTtlSeconds(accessToken);
            if (ttlSeconds > 0) {
                redis.blacklistAccessToken(accessToken, ttlSeconds);
            }
        } catch (Exception e) {
            // Log error but don't fail logout if token blacklisting fails
            logger.warn("Failed to blacklist access token during logout", e);
        }

        exchange.getResponseSender().send("{\"message\":\"logged out successfully\"}");
    }

    public AuthSession createSessionFromCred(AuthCredential cred) {
        long refreshTtlMs = Duration.ofDays(7).toMillis();

        AuthSession authSession = new AuthSession(UUID.randomUUID(), cred.getId(), UUID.randomUUID().toString(), "password", OffsetDateTime.now().plus(refreshTtlMs, ChronoUnit.MILLIS));

        authDao.sessionDao.insert(authSession);

        return authSession;
    }

    // create server session: stores session in DB and Redis
    void addSessionToRedis(AuthSession authSession) throws Exception {
        // store in Redis: key refresh:<token> -> userId|profile
        redis.putRefreshToken(authSession.getSessionToken(), authSession.getCredId().toString(), "customer", Duration.between(OffsetDateTime.now(), authSession.getExpiresAt()).toMillis());
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

        String newAccess = jwt.generateAccessToken(info.get("sessionId"), info.get("credId"), info.get("userId"), info.get("profile"));
        exchange.getResponseSender().send(mapper.writeValueAsString(Map.of("access_token", newAccess)));
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
