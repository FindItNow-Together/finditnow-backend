package com.finditnow.config;

import io.github.cdimascio.dotenv.Dotenv;

public class Config {

    private static final Dotenv SERVICE_ENV;
    private static final Dotenv ROOT_ENV;

    static {
        // Load service-level env (default: current working dir)
        SERVICE_ENV = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        // Load project root env (go up two levels from services)
        ROOT_ENV = Dotenv.configure()
                .directory("../../")
                .ignoreIfMissing()
                .load();
    }

    public static String get(String key, String defaultValue) {
        String value = SERVICE_ENV.get(key);

        if (value == null) value = ROOT_ENV.get(key);
        if (value == null) value = System.getenv(key);

        return value != null ? value : defaultValue;
    }

    public static String get(String key) {
        return get(key, null);
    }
}

