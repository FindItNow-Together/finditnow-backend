package com.finditnow.config;

import io.github.cdimascio.dotenv.Dotenv;

public class Config {
    private static final Dotenv ENV = Dotenv.load();
    
    public static String get(String key, String defaultValue) {
        return ENV.get(key, defaultValue);
    }
    
    public static String get(String key) {
        return ENV.get(key);
    }
}

