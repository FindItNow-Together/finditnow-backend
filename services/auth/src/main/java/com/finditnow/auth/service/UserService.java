package com.finditnow.auth.service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.finditnow.dispatcher.EmailDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finditnow.auth.db.UserDao;
import com.finditnow.auth.model.User;
import com.finditnow.common.OtpGenerator;
import com.finditnow.common.PasswordUtil;
import com.finditnow.jwt.JwtService;
import com.finditnow.mail.MailService;
import com.finditnow.redis.RedisStore;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.Headers;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final EmailDispatcher mailer = new EmailDispatcher(new MailService());
    private final ObjectMapper mapper = new ObjectMapper();
    private final UserDao userDao;
    private final RedisStore redis;
    private final JwtService jwt;

    public UserService(UserDao userDao, RedisStore redis, JwtService jwt) {
        this.userDao = userDao;
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

        if (email != null && userDao.findByEmail(email) != null) {
            exchange.setStatusCode(409);
            exchange.getResponseSender().send("{\"error\":\"user_exists\"}");
            return;
        }

        String id = UUID.randomUUID().toString();
        String pwHash = PasswordUtil.hash(password);

        User user = new User(id, username, email, phone, pwHash);

        userDao.save(user);

        String emailOtp = OtpGenerator.generateSecureOtp(6);

        mailer.send(email, "FindItNow: Email Verification", String
                .format("Your email verification code: <strong style=\"font-size:18px\">%s</strong>.<br><p style=\"font-weight:700\">This email is system generated. Do not reply</p>",
                        emailOtp), true);

        redis.setKey("emailOtp:" + id, String.valueOf(emailOtp), 2 * 60L);

        exchange.setStatusCode(201);
        exchange.getResponseSender().send("{\"message\":\"user_created\",\"user_id\":\"" + id + "\"}");
    }

    public void verifyEmail(HttpServerExchange exchange) throws Exception{
        String body = new String(exchange.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> bodyMap = mapper.readValue(body, Map.class);

        String reqUserId = bodyMap.get("userId");
        String verificationOtp = bodyMap.get("verificationCode");

        if(reqUserId==null || verificationOtp == null){
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"missing_fields\"}");
            return;
        }

        String storedOtp = redis.getKeyValue("emailOtp:"+reqUserId);

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
        userDao.updateUserById(reqUserId, updateFields);

        exchange.setStatusCode(201);
        exchange.getResponseSender().send("{\"message\":\"user email verified\",\"user_id\":\"" + reqUserId + "\"}");
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

        User user  = userDao.findByEmail(email);

        if(user==null){
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"email not registered\"}");
            return;
        }

        String userId = user.getId();

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

        User user = userDao.findByIdentifier(identifier);
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
        String dbRefreshToken = userDao.logoutSession(tokenUserInfo.get("userId"), tokenUserInfo.get("profile"));
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
        userDao.insertSession(UUID.randomUUID().toString(), userId, refreshToken, authMethod, ttlMillis, true, profile);
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
        User u = userDao.findByEmail(email);
        if (u != null)
            return u.getId();
        String id = UUID.randomUUID().toString();
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        userDao.save(user);
        return id;
    }
}
