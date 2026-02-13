package com.example.text2cypher.utils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

public final class LocalMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private LocalMapper() {}

    public static String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to write object as JSON",
                    e
            );
        }
    }

    public static <T> T read(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to read JSON into " + clazz.getSimpleName(),
                    e
            );
        }
    }

    public static <T> List<T> readList(String json, Class<T> elementType) {
        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, elementType)
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to read JSON list",
                    e
            );
        }
    }
}

