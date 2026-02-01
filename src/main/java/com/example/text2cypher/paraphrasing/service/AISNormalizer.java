package com.example.text2cypher.paraphrasing.service;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AISNormalizer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    public Map<String, Object> normalizeAIS(String llmOutput) {
        String cleaned = preprocess(llmOutput);
        String json = extractJson(cleaned);

        Map<String, Object> raw;
        try {
            raw = objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse JSON from LLM output", e);
        }
        return raw;
    }
    private String preprocess(String raw) {
        String s = raw;
        s = s.replaceAll("(?s)<think>.*?</think>", "");
        s = s.replaceAll("(?s)<analysis>.*?</analysis>", "");
        // Remove opening ``` or ```json
        s = s.replaceAll("(?m)^```\\s*json\\s*$", "");
        s = s.replaceAll("(?m)^```\\s*$", "");

        // Remove closing ```
        s = s.replaceAll("(?m)^```\\s*$", "");
        return s.trim();
    }
    private String extractJson(String text) {
        System.out.println(text);
        if (text.startsWith("[") && text.contains("{")) {
            int objStart = text.indexOf("{");
            int objEnd = text.lastIndexOf("}");
            return text.substring(objStart, objEnd + 1);
        }
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        throw new IllegalStateException("No JSON object found");
    }
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castToList(Object node) {
        if (node instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    out.add((Map<String, Object>) map);
                }
            }
            return out;
        }
        throw new IllegalStateException("Cannot cast to list of maps: " + node);
    }
}
