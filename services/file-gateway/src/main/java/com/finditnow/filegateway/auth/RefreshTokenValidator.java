package com.finditnow.filegateway.auth;

import com.finditnow.filegateway.exceptions.UnauthorizedException;
import com.finditnow.filegateway.models.FileRequestContext;
import com.finditnow.redis.RedisStore;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.AttachmentKey;

import java.util.Map;

public class RefreshTokenValidator {
    private final RedisStore redis;

    public RefreshTokenValidator(RedisStore redis) {
        this.redis = redis;
    }

    public static final AttachmentKey<FileRequestContext> CONTEXT =
            AttachmentKey.create(FileRequestContext.class);

    public void validate(HttpServerExchange exchange) {
        Cookie tokenCookie = exchange.getRequestCookie("refresh_token");
        if (tokenCookie == null) {
            throw new UnauthorizedException();
        }

        String token = tokenCookie.getValue();

        Map<String, String> tokenData = redis.getRefreshToken(token);

        if (tokenData == null) {
            throw new UnauthorizedException();
        }

        exchange.putAttachment(CONTEXT, new FileRequestContext(tokenData.get("userId")));
    }
}
