package com.finditnow.auth.service;

import com.finditnow.auth.dao.AuthDao;
import com.finditnow.auth.dto.AuthResponse;
import com.finditnow.auth.dto.SignUpDto;
import com.finditnow.auth.model.AuthCredential;
import com.finditnow.auth.model.AuthOauthGoogle;
import com.finditnow.auth.model.AuthSession;
import com.finditnow.auth.transaction.TransactionManager;
import com.finditnow.auth.types.Role;
import com.finditnow.auth.utils.Logger;
import com.finditnow.common.OtpGenerator;
import com.finditnow.common.PasswordUtil;
import com.finditnow.config.Config;
import com.finditnow.dispatcher.EmailDispatcher;
import com.finditnow.jwt.JwtService;
import com.finditnow.mail.MailService;
import com.finditnow.redis.RedisStore;
import com.finditnow.user.CreateUserProfileRequest;
import com.finditnow.user.UpdateUserRoleRequest;
import com.finditnow.user.UserServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AuthService {
    private static final Logger logger = Logger.getLogger(AuthService.class);
    private static final EmailDispatcher mailer = new EmailDispatcher(new MailService());
    private final AuthDao authDao;
    private final RedisStore redis;
    private final JwtService jwt;
    private final TransactionManager transactionManager;
    private final long refreshTokenMaxLifeSeconds = Duration.ofDays(7).toSeconds();

    public AuthService(AuthDao authDao, RedisStore redis, JwtService jwt) {
        this.authDao = authDao;
        this.redis = redis;
        this.jwt = jwt;
        this.transactionManager = new TransactionManager(authDao.getDataSource());
    }

    public AuthResponse signUp(SignUpDto signUpReq) {
        try {
            return transactionManager.executeInTransaction(conn -> {
                // Check for existing credential
                Optional<AuthCredential> existingCred = authDao.credDao.findByEmail(conn, signUpReq.getEmail());

                if (existingCred.isPresent()) {
                    AuthCredential cred = existingCred.get();
                    Map<String, String> data = new HashMap<>();

                    if (cred.isEmailVerified()) {
                        data.put("error", "user_already_verified");
                        return new AuthResponse(409, data);
                    } else {
                        data.put("error", "account_not_verified");
                        data.put("credId", cred.getId().toString());
                        return new AuthResponse(400, data);
                    }
                }

                // Create new credential
                UUID credId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                String pwHash = PasswordUtil.hash(signUpReq.getPassword());

                AuthCredential cred = new AuthCredential(credId, userId, signUpReq.getEmail(), signUpReq.getPhone(), pwHash, signUpReq.getRole(), false, false, OffsetDateTime.now());
                cred.setFirstName(signUpReq.getFirstName());

                authDao.credDao.insert(conn, cred);

                // Send verification email (outside transaction)
                String emailOtp = sendVerificationEmail(signUpReq.getEmail(), credId.toString());

                Map<String, String> data = new HashMap<>();
                data.put("credId", credId.toString());
                data.put("message", "verification email sent");
                data.put("accessTokenValiditySeconds", "120");

                if (Config.get("ENVIRONMENT", "development").equals("development")) {
                    data.put("emailOtp", emailOtp);
                }

                return new AuthResponse(201, data);
            });

        } catch (Exception e) {
            logger.getCore().error("Signup failed", e);
            Map<String, String> data = new HashMap<>();
            data.put("error", "internal_server_error");
            return new AuthResponse(500, data);
        }
    }

    public AuthResponse verifyEmail(String credId, String verificationOtp) {
        Map<String, String> data = new HashMap<>();
        String storedOtp = redis.getKeyValue("emailOtp:" + credId);

        if (storedOtp == null) {
            data.put("error", "otp_not_found");
            return new AuthResponse(400, data);
        }

        if (!storedOtp.equals(verificationOtp)) {
            data.put("error", "invalid_otp");
            return new AuthResponse(400, data);
        }

        try {
            return transactionManager.executeInTransaction(conn -> {
                // Update email verification status
                Map<String, Object> updateFields = new HashMap<>();
                updateFields.put("is_email_verified", true);
                authDao.credDao.updateCredFieldsById(conn, UUID.fromString(credId), updateFields);

                // Fetch updated credential
                Optional<AuthCredential> authCred = authDao.credDao.findById(conn, UUID.fromString(credId));
                if (authCred.isEmpty()) {
                    data.put("error", "credential_not_found");
                    return new AuthResponse(404, data);
                }

                AuthCredential cred = authCred.get();

                // Create user profile (can be done outside transaction if needed)
                try {
                    createUserProfile(cred);
                } catch (Exception e) {
                    logger.getCore().error("Failed to create user profile", e);
                    throw new RuntimeException("User profile creation failed", e);
                }

                // Create session
                AuthSession authSession = createSessionInTransaction(conn, cred, "password");

                // Generate tokens
                String accessToken = jwt.generateAccessToken(authSession.getId().toString(), cred.getId().toString(), cred.getUserId().toString(), cred.getRole().toString());

                // Store in Redis (outside DB transaction)
                try {
                    addSessionToRedis(authSession, cred.getUserId().toString(), cred.getRole().toString());
                } catch (Exception e) {
                    logger.getCore().error("Failed to add session to Redis", e);
                    throw new RuntimeException("Redis operation failed", e);
                }

                data.put("accessToken", accessToken);
                data.put("profile", cred.getRole().toString());
                data.put("firstName", cred.getFirstName());
                data.put("sessionToken", authSession.getSessionToken());

                return new AuthResponse(200, data);
            });

        } catch (Exception e) {
            logger.getCore().error("Email verification failed", e);
            data.put("error", "internal_server_error");
            return new AuthResponse(500, data);
        }
    }

    public AuthResponse resetPassword(String email, String phone, String password) {
        Map<String, String> data = new HashMap<>();

        if (redis.getKeyValue("resetAllowed:" + email) == null) {
            data.put("error", "reset window exceeded");
            return new AuthResponse(400, data);
        }

        try {
            return transactionManager.executeInTransaction(conn -> {
                Optional<AuthCredential> credOpt = authDao.credDao.findByEmail(conn, email);
                if (credOpt.isEmpty()) {
                    data.put("error", "credential_not_found");
                    return new AuthResponse(404, data);
                }

                AuthCredential cred = credOpt.get();
                Map<String, Object> updateFields = new HashMap<>();
                updateFields.put("password_hash", PasswordUtil.hash(password));
                authDao.credDao.updateCredFieldsById(conn, cred.getId(), updateFields);

                // Clean up Redis keys (outside transaction)
                redis.deleteKey("resetAllowed:" + email);

                data.put("message", "password updated");
                return new AuthResponse(200, data);
            });

        } catch (Exception e) {
            logger.getCore().error("Password reset failed", e);
            data.put("error", "internal_server_error");
            return new AuthResponse(500, data);
        }
    }

    public AuthResponse updateRoleByCredential(UUID credId, String userId, String role) {
        Map<String, Object> toUpdate = new HashMap<>();
        toUpdate.put("role", role);
        Map<String, String> resp = new HashMap<>();

        try {
            transactionManager.executeInTransaction(conn -> {
                AuthCredential cred = authDao.credDao.findById(credId).orElseThrow(()-> new RuntimeException("Credential not found"));

                cred.setRole(Role.valueOf(role));
                authDao.credDao.update(conn, cred);
                return credId;
            });

            updateUserRole(userId, role);
            resp.put("message", "role_updated");
            return new AuthResponse(201, resp);
        } catch (Exception e) {
            logger.getCore().error("Failed to update role", e);
            resp.put("error", "internal_server_error");
            return new AuthResponse(500, resp);
        }
    }

    public AuthResponse signIn(String identifier, String password) {
        Map<String, String> data = new HashMap<>();

        try {
            return transactionManager.executeInTransaction(conn -> {
                // Find credential
                Optional<AuthCredential> authCred = authDao.credDao.findByIdentifier(conn, identifier);

                if (authCred.isEmpty()) {
                    data.put("error", "invalid credentials");
                    return new AuthResponse(401, data);
                }

                AuthCredential cred = authCred.get();

                if (!cred.isEmailVerified() && !cred.isPhoneVerified()) {
                    data.put("error", "account_not_verified");
                    data.put("credId", cred.getId().toString());
                    return new AuthResponse(403, data);
                }

                if (!PasswordUtil.verifyPassword(password, cred.getPasswordHash())) {
                    data.put("error", "invalid credentials");
                    return new AuthResponse(401, data);
                }

                // Create session
                AuthSession authSession = createSessionInTransaction(conn, cred, "password");

                String accessToken = jwt.generateAccessToken(authSession.getId().toString(), cred.getId().toString(), cred.getUserId().toString(), cred.getRole().toString());

                // Store in Redis
                try {
                    addSessionToRedis(authSession, cred.getUserId().toString(), cred.getRole().toString());
                } catch (Exception e) {
                    throw new RuntimeException("Redis operation failed", e);
                }

                data.put("accessToken", accessToken);
                data.put("profile", cred.getRole().toString());
                data.put("firstName", cred.getFirstName());
                data.put("sessionToken", authSession.getSessionToken());

                return new AuthResponse(200, data);
            });

        } catch (Exception e) {
            logger.getCore().error("Sign in failed", e);
            data.put("error", "internal_server_error");
            return new AuthResponse(500, data);
        }
    }

    public AuthCredential findOrCreateUserByEmail(String email) {
        try {
            return transactionManager.executeInTransaction(conn -> {
                Optional<AuthCredential> existing = authDao.credDao.findByEmail(conn, email);

                if (existing.isPresent()) {
                    return existing.get();
                }

                // Create new credential for OAuth
                AuthCredential cred = createCredentialFromEmail(email, conn);

                return cred;
            });

        } catch (Exception e) {
            throw new RuntimeException("Failed to find or create user", e);
        }
    }

    public AuthCredential findCredentialByAuthProvider(String oauthSub) {
        try {
            return transactionManager.executeInTransaction(conn -> authDao.credDao.findByOauthSubject(conn, oauthSub).orElse(null));
        } catch (Exception e) {
            throw new RuntimeException("Failed to credential by oauth subject", e);
        }
    }

    /**
     * @param oauthSub subject for the oauth provider unique
     * @param email    email used for the authentication
     * @return AuthCredential newly created
     */
    public AuthCredential findOrCreateCredWithOauth(String oauthSub, String email) {
        try {
            return transactionManager.executeInTransaction(conn -> {
                AuthCredential cred = createCredentialFromEmail(email, conn);

                AuthOauthGoogle authGoogleRecord = new AuthOauthGoogle();
                authGoogleRecord.setId(UUID.randomUUID());
                authGoogleRecord.setGoogleUserId(oauthSub);
                authGoogleRecord.setEmail(email);
                authGoogleRecord.setUserId(cred.getUserId());

                authDao.oauthDao.insert(conn, authGoogleRecord);

                return cred;
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to find or create user", e);
        }
    }

    private AuthCredential createCredentialFromEmail(String email, Connection conn) throws SQLException {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AuthCredential cred = new AuthCredential();
        cred.setId(id);
        cred.setUserId(userId);
        cred.setEmailVerified(true);
        cred.setFirstName(email);
        cred.setEmail(email);
        cred.setRole(Role.UNASSIGNED);

        authDao.credDao.insert(conn, cred);

        // Create user profile
        try {
            createUserProfile(cred);
        } catch (Exception e) {
            logger.getCore().warn("Failed to create user profile for OAuth user", e);
        }

        return cred;
    }

    // Helper methods
    private AuthSession createSessionInTransaction(Connection conn, AuthCredential cred, String sessionMethod) throws Exception {
        AuthSession authSession = new AuthSession(UUID.randomUUID(), cred.getId(), UUID.randomUUID().toString(), sessionMethod, OffsetDateTime.now().plusSeconds(refreshTokenMaxLifeSeconds));

        authDao.sessionDao.insert(conn, authSession);
        return authSession;
    }

    public void addSessionToRedis(AuthSession authSession, String userId, String role) throws Exception {
        redis.putRefreshToken(authSession.getSessionToken(), authSession.getId().toString(), authSession.getCredId().toString(), userId, role, Duration.between(OffsetDateTime.now(), authSession.getExpiresAt()).toMillis());
    }

    private String sendVerificationEmail(String email, String credId) {
        String emailOtp = OtpGenerator.generateSecureOtp(6);

        mailer.send(email, "FindItNow: Email Verification", String.format("Your email verification code: <strong style=\"font-size:18px\">%s</strong>.<br>" + "<p style=\"font-weight:700\">This email is system generated. Do not reply</p>", emailOtp), true);

        redis.setKey("emailOtp:" + credId, emailOtp, 2 * 60L);
        return emailOtp;
    }

    private void createUserProfile(AuthCredential cred) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(Config.get("USER_SERVICE_GRPC_HOST", "localhost"), Integer.parseInt(Config.get("USER_SERVICE_GRPC_PORT", "8083"))).usePlaintext().build();

        try {
            UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
            var res = stub.createUserProfile(CreateUserProfileRequest.newBuilder().setId(cred.getUserId().toString()).setEmail(cred.getEmail()).setName(cred.getFirstName()).setRole(cred.getRole().toString()).build());

            if (!res.hasUser()) {
                throw new RuntimeException("User profile creation failed");
            }
        } finally {
            channel.shutdown();
        }
    }

    private void updateUserRole(String userId, String role) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(Config.get("USER_SERVICE_GRPC_HOST", "localhost"), Integer.parseInt(Config.get("USER_SERVICE_GRPC_PORT", "8083"))).usePlaintext().build();

        try {
            UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
            var res = stub.updateUserRole(UpdateUserRoleRequest.newBuilder().setId(userId).setRole(role).build());

            if (!res.hasError()) {
                throw new RuntimeException("User role update failed for id " + userId);
            }
        } finally {
            channel.shutdown();
        }
    }

    public AuthResponse resendVerificationEmail(String credId) {
        Map<String, String> data = new HashMap<>();
        Optional<AuthCredential> authCred;
        try {
            authCred = authDao.credDao.findById(UUID.fromString(credId));
        } catch (SQLException e) {
            logger.getCore().error(e.getMessage());
            data.put("error", "internal server error");
            return new AuthResponse(500, data);
        }

        if (authCred.isEmpty()) {
            data.put("error", "email_not_registered");
            return new AuthResponse(400, data);
        }

        AuthCredential cred = authCred.get();

        if (cred.isEmailVerified()) {
            data.put("error", "email_verified");
            return new AuthResponse(400, data);
        }

        String emailOtp = sendVerificationEmail(cred.getEmail(), credId);

        data.put("message", "verification email sent");
        data.put("accessTokenValiditySeconds", "120");

        if (Config.get("ENVIRONMENT", "development").equals("development")) {
            data.put("emailOtp", emailOtp);
        }

        return new AuthResponse(201, data);
    }

    public AuthResponse sendResetPwdToken(String email, String phone) {
        Map<String, String> data = new HashMap<>();

        Optional<AuthCredential> authCred;
        try {
            authCred = authDao.credDao.findByEmail(email);
        } catch (SQLException e) {
            logger.getCore().error(e.getMessage());
            data.put("error", "internal server error");
            return new AuthResponse(500, data);
        }

        if (authCred.isEmpty()) {
            data.put("error", "email not registered");
            return new AuthResponse(400, data);
        }

        String resetOtp = OtpGenerator.generateSecureOtp(8);
        mailer.send(email, "Reset Password Token:Finditnow", String.format("Your reset password token: <strong style=\"font-size:18px\">%s</strong>.<br>" + "<p style=\"font-weight:700\">This email is system generated. Do not reply</p>", resetOtp), true);

        redis.setKey("resetOtp:" + email, resetOtp, Duration.ofMinutes(5).toSeconds());

        data.put("message", "verification email resent");
        data.put("tokenValiditySeconds", "300");

        return new AuthResponse(200, data);
    }

    public AuthResponse verifyResetToken(String email, String phone, String token) {
        Map<String, String> data = new HashMap<>();
        String storedToken = redis.getKeyValue("resetOtp:" + email);

        if (storedToken == null || !storedToken.equals(token)) {
            data.put("error", "invalid_token");
            return new AuthResponse(400, data);
        }

        redis.deleteKey("resetOtp:" + email);
        redis.setKey("resetAllowed:" + email, String.valueOf(true), Duration.ofMinutes(2).toSeconds());

        data.put("message", "token_verified");
        return new AuthResponse(200, data);
    }

    public AuthResponse updatePassword(String credId, String newPassword) {
        Map<String, String> data = new HashMap<>();

        if (!PasswordUtil.checkPwdString(newPassword)) {
            data.put("error", "password not in desired format");
            return new AuthResponse(400, data);
        }

        try {
            return transactionManager.executeInTransaction(conn -> {
                Map<String, Object> updateFields = new HashMap<>();
                updateFields.put("password_hash", PasswordUtil.hash(newPassword));

                authDao.credDao.updateCredFieldsById(conn, UUID.fromString(credId), updateFields);

                data.put("message", "password updated");
                return new AuthResponse(200, data);
            });
        } catch (Exception e) {
            logger.getCore().error(e.getMessage());
            data.put("error", "internal server error");
            return new AuthResponse(500, data);
        }
    }

    public AuthResponse logout(String accessToken, String refreshToken) {
        Map<String, String> data = new HashMap<>();

        if (refreshToken != null) {
            try {
                transactionManager.executeInTransaction(conn -> {
                    authDao.sessionDao.invalidateByToken(conn, refreshToken);
                    return null;
                });

                redis.deleteRefreshToken(refreshToken);
            } catch (Exception e) {
                logger.getCore().warn("Failed to invalidate refresh token", e);
            }
        }

        if (accessToken != null) {
            try {
                long ttlSeconds = jwt.getTokenRemainingTtlSeconds(accessToken);
                if (ttlSeconds > 0) {
                    redis.blacklistAccessToken(accessToken, ttlSeconds);
                }
            } catch (Exception e) {
                logger.getCore().warn("Failed to blacklist access token during logout", e);
            }
        }

        data.put("message", "logged out successfully");
        return new AuthResponse(200, data);
    }

    public AuthResponse refresh(String refreshToken) {
        Map<String, String> data = new HashMap<>();
        Map<String, String> info = redis.getRefreshToken(refreshToken);

        if (info == null) {
            data.put("error", "invalid_refresh");
            return new AuthResponse(401, data);
        }

        String newAccess = jwt.generateAccessToken(info.get("sessionId"), info.get("credId"), info.get("userId"), info.get("profile"));

        data.put("accessToken", newAccess);
        data.put("profile", info.get("profile"));

        return new AuthResponse(200, data);
    }

    public AuthSession createSessionFromCred(AuthCredential cred, String sessionMethod) throws Exception {
        return transactionManager.executeInTransaction(conn -> {
            AuthSession authSession = new AuthSession(UUID.randomUUID(), cred.getId(), UUID.randomUUID().toString(), sessionMethod, // or pass as parameter to distinguish oauth vs password
                    OffsetDateTime.now().plusSeconds(refreshTokenMaxLifeSeconds));

            authDao.sessionDao.insert(conn, authSession);
            return authSession;
        });
    }
}
