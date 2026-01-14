package com.example.text2cypher.paraphrasing.service;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ParaphraseNormalizer {
    public static String normalize(String content, String model) {
        content = preprocess(model, content);
        return Arrays.stream(content.split("\n"))
                .map(s -> s.replaceAll("^\\d+\\.\\s*", "").trim())
                .filter(s -> !s.isEmpty())
                .findFirst()
                .orElse("");
    }
    public static String preprocess(String model, String content) {
        if (content == null) return "";

        if (model.startsWith("qwen/")) {
            content = content.replaceAll("(?s)<think>.*?</think>", "");
        }

        return content.trim();
    }

}
