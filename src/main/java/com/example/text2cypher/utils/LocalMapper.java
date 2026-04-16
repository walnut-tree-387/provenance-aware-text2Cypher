package com.example.text2cypher.utils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
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
    public static <T> List<T> readListOneByOne(JsonNode root, Class<T> elementType) {
        List<T> result = new ArrayList<>();
        try {
            if (!root.isArray()) {
                throw new IllegalArgumentException("Expected JSON array");
            }
            for (JsonNode node : root) {
                try {
                    T target = elementType.getDeclaredConstructor().newInstance();
                    objectMapper.readerForUpdating(target).readValue(node);
                    result.add(target);
                } catch (Exception e) {
                    try {
                        result.add(elementType.getDeclaredConstructor().newInstance());
                    } catch (Exception reflectionError) {
                        System.out.println("Skipping invalid AIS node: " + node);
                    }
                }
            }

        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse JSON array", e);
        }
        return result;
    }
    public static JsonNode convertToJsonNode(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to convert object to JsonNode",
                    e
            );
        }
    }
}

