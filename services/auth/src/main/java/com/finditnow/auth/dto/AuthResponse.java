package com.finditnow.auth.dto;

import java.util.Map;

public record AuthResponse(int statusCode, Map<String, String> data) {

    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    public Map<String, String> getData() {
        return data;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
