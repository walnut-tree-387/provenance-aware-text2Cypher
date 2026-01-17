package com.example.text2cypher.paraphrasing.service;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class LLMAnswerNormalizer {
    public static String paraphraseNormalize(String content, String model) {
        content = preprocess(model, content);
        return Arrays.stream(content.split("\n"))
                .map(s -> s.replaceAll("^\\d+\\.\\s*", "").trim())
                .filter(s -> !s.isEmpty())
                .findFirst()
                .orElse("");
    }
    public static String cypherNormalize(String content, String model) {
        content = preprocess(model, content);
        content = normalizeWhitespace(content);
        return content;
    }
    private static String normalizeWhitespace(String content) {
        if (content == null) return "";

        return content
                .replaceAll("[\\t ]+", " ")        // collapse spaces & tabs
                .replaceAll("\\s*\\n\\s*", "\n")   // clean newlines
                .replaceAll("\\n{2,}", "\n")       // remove empty lines
                .trim();
    }
    public static String preprocess(String model, String content) {
        if (content == null) return "";

        if (model.startsWith("qwen/")) {
            content = content.replaceAll("(?s)<think>.*?</think>", "");
        }

        return content.trim();
    }

}
