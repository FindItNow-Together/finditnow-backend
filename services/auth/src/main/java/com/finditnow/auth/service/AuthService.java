package com.finditnow.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finditnow.auth.dao.AuthCredentialDao;
import com.finditnow.auth.dao.AuthDao;
import com.finditnow.auth.model.AuthCredential;
import com.finditnow.auth.model.User;
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
import java.time.OffsetDateTime;
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

        AuthCredential cred = new AuthCredential(credId,userId, email, phone, pwHash, false, false, OffsetDateTime.now());

        authDao.credDao.insert(cred);

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 8082)
                .usePlaintext()
                .build();

        UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);

        var res = stub.createUserProfile(
                CreateUserProfileRequest.newBuilder()
                        .setId(credId.toString())
                        .setEmail(email)
                        .setName(username)
                        .build()
        );

        String emailOtp = OtpGenerator.generateSecureOtp(6);

        mailer.send(email, "FindItNow: Email Verification", String
                .format("Your email verification code: <strong style=\"font-size:18px\">%s</strong>.<br><p style=\"font-weight:700\">This email is system generated. Do not reply</p>",
                        emailOtp), true);

        redis.setKey("emailOtp:" + userId, String.valueOf(emailOtp), 2 * 60L);

        exchange.setStatusCode(201);
        exchange.getResponseSender().send("{\"message\":\"user_created\",\"user_id\":\"" + userId + "\"}");
    }

    public void verifyEmail(HttpServerExchange exchange) throws Exception{
        String body = new String(exchange.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> bodyMap = mapper.readValue(body, Map.class);

        String credId = bodyMap.get("credId");
        String verificationOtp = bodyMap.get("verificationCode");

        if(credId==null || verificationOtp == null){
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"missing_fields\"}");
            return;
        }

        String storedOtp = redis.getKeyValue("emailOtp:"+credId);

        if(storedOtp==null){
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"otp not found\"}");
            return;
        }

        if(!storedOtp.equals(verificationOtp)){
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"invalid otp\"}");
            return;
        }

        Map<String, Object> updateFields = new HashMap<>();

        updateFields.put("isEmailVerified", true);
        authDao.credDao.updateCredFieldsById(credId, updateFields);

        exchange.setStatusCode(201);
        exchange.getResponseSender().send("{\"message\":\"user email verified\",\"user_id\":\"" + credId + "\"}");
    }

    public void resendVerificationEmail(HttpServerExchange exchange) throws Exception{
        String body = new String(exchange.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> bodyMap = mapper.readValue(body, Map.class);
        String email = bodyMap.get("email");

        //email is required for sending verification email
        if(email==null || email.isEmpty()){
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"missing_fields\"}");
            return;
        }

        if(!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")){
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"invalid email\"}");
            return;
        }

        Optional<AuthCredential> cred  = authDao.credDao.findByEmail(email);

        if(cred.isEmpty()){
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"email not registered\"}");
            return;
        }

        String userId = cred.get().getUserId().toString();

        String emailOtp = OtpGenerator.generateSecureOtp(6);

        mailer.send(email, "FindItNow: Email Verification", String
                .format("Your email verification code: <strong style=\"font-size:18px\">%s</strong>.<br><p style=\"font-weight:700\">This email is system generated. Do not reply</p>",
                        emailOtp), true);

        redis.setKey("emailOtp:" + userId, String.valueOf(emailOtp), 2 * 60L);
        exchange.setStatusCode(201);
        exchange.getResponseSender().send("{\"message\":\"user_created\",\"user_id\":\"" + userId + "\"}");
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

        User user = authDao.credDao.findByIdentifier(identifier);
        if (user == null || !PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            exchange.setStatusCode(401);
            exchange.getResponseSender().send("{\"error\":\"invalid_credentials\"}");
            return;
        }

        String accessToken = jwt.generateAccessToken(user.getId(), authProfile);
        long refreshTtl = 7L * 24 * 60 * 60 * 1000L; // 7 days
        String refreshToken = createSession(user.getId(), "password", authProfile, refreshTtl);

        Map<String, String> resp = new HashMap<>();
        resp.put("access_token", accessToken);
        exchange.getResponseHeaders().put(Headers.SET_COOKIE,
                "refresh_token=" + refreshToken + ";Secure; Path=/refresh; HttpOnly; SameSite=Strict");
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
            accessToken = authHeader.split(" ", 2)[1];
        } catch (Exception e) {
            exchange.setStatusCode(401);
            exchange.getResponseSender().send("{\"error\":\"request unauthorized\"}");
            return;
        }

        Map<String, String> tokenUserInfo = jwt.parseTokenToUser(accessToken);

        // Delete refresh token from DB and Redis
        String dbRefreshToken = authDao.credDao.logoutSession(tokenUserInfo.get("userId"), tokenUserInfo.get("profile"));
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

    // create server session: stores session in DB and Redis
    public String createSession(String userId, String authMethod, String profile, long ttlMillis) throws Exception {
        String refreshToken = UUID.randomUUID().toString();
        // store in Redis: key refresh:<token> -> userId|profile
        redis.putRefreshToken(refreshToken, userId, profile, ttlMillis);
        // also store session in DB for audit (best-effort; wrap in try/catch)
        authDao.sessionDao.insertSession(UUID.randomUUID().toString(), userId, refreshToken, authMethod, ttlMillis, true, profile);
        return refreshToken;
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

        String userId = info.get("userId");
        String profile = info.get("profile");

        String newAccess = jwt.generateAccessToken(userId, profile);
        exchange.getResponseSender().send(mapper.writeValueAsString(Map.of("access_token", newAccess)));
    }

    // used by OAuth service to find or create user by email
    public String findOrCreateUserByEmail(String email) throws Exception {
        Optional<AuthCredential> u = authDao.credDao.findByEmail(email);
        UUID id = UUID.randomUUID();
        AuthCredential cred = new AuthCredential();
        cred.setUuid(id);
        cred.setEmail(email);
        authDao.credDao.insert(cred);
        return id.toString();
    }
}
