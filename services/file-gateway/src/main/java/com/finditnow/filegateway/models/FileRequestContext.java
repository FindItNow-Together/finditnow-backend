package com.finditnow.filegateway.models;

public class FileRequestContext {

    private final String userId;

    public FileRequestContext(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}