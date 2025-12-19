package com.finditnow.auth.utils;

import com.finditnow.auth.handlers.PathHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.LoggerFactory;


public class Logger {
    private org.slf4j.Logger logger;

    private Logger(Class<?> clazz) {
        logger = LoggerFactory.getLogger(clazz);
    }

    public static Logger getLogger(Class<?> clazz) {
        return new Logger(clazz);
    }

    public void logResponse(HttpServerExchange exchange) {
        int statusCode = exchange.getStatusCode();
        String method = exchange.getRequestMethod().toString();
        String path = exchange.getRequestPath();

        // Calculate execution time
        Long startTime = exchange.getAttachment(PathHandler.REQUEST_START_TIME_KEY);
        long executionTimeMs = 0;
        if (startTime != null) {
            executionTimeMs = (System.nanoTime() - startTime) / 1_000_000; // Convert nanoseconds to milliseconds
        }

        if (statusCode >= 200 && statusCode < 300) {
            logger.info("Response: {} {} {} (SUCCESS) - {}ms", method, path, statusCode, executionTimeMs);
        } else if (statusCode >= 400 && statusCode < 500) {
            logger.warn("Response: {} {} {} (CLIENT_ERROR) - {}ms", method, path, statusCode, executionTimeMs);
        } else if (statusCode >= 500) {
            logger.error("Response: {} {} {} (SERVER_ERROR) - {}ms", method, path, statusCode, executionTimeMs);
        } else {
            logger.info("Response: {} {} {} - {}ms", method, path, statusCode, executionTimeMs);
        }
    }

    public void error(String message) {
        logger.error(message);
    }

    public org.slf4j.Logger getCore() {
        return logger;
    }
}
