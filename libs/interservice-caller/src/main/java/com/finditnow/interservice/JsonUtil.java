package com.finditnow.interservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtil {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String toJson(Object object){
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("failed to serialize object", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        try {
            return mapper.readValue(json, classOfT);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("failed to deserialize json", e);
        }
    }
}
