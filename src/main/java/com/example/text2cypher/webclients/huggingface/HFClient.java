package com.example.text2cypher.webclients.huggingface;

import com.example.text2cypher.webclients.dto.ChatRequest;
import com.example.text2cypher.webclients.dto.ClientMessage;
import com.example.text2cypher.webclients.dto.GroqChatResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
@Component
public class HFClient {
    private final WebClient webClient;
    private final List<String> apiKeys;
    private final AtomicInteger keyIndex = new AtomicInteger(0);

    public HFClient(WebClient huggingFaceWebClient, List<String> huggingFaceApiKeys) {
        this.webClient = huggingFaceWebClient;
        this.apiKeys = huggingFaceApiKeys;
    }

    public GroqChatResponse chatCompletion(double temperature, String prompt, String model) {
        int maxRetries = apiKeys.size();
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            String currentKey = getActiveKey();
            try {
                return webClient.post()
                        .uri("/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + currentKey)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .bodyValue(buildRequest(temperature, prompt, model))
                        .retrieve()
                        .bodyToMono(GroqChatResponse.class)
                        .block();
            } catch (Exception e) {
                System.err.println("Key " + (keyIndex.get() % apiKeys.size()) + " failed. Rotating...");
                rotateKey();
            }
        }
        return null;
    }

    private String getActiveKey() {
        return apiKeys.get(Math.abs(keyIndex.get() % apiKeys.size()));
    }

    private void rotateKey() {
        keyIndex.incrementAndGet();
    }

    private ChatRequest buildRequest(double temp, String prompt, String model) {
        ChatRequest request = ChatRequest.builder()
                .model(model)
                .messages(List.of(
                        new ClientMessage("system", "You are a strict JSON generator. Output ONLY valid JSON list containing AIS List for each of questions sequentially." +
                                " Do NOT include reasoning, explanations, or <think> tags. If you include anything else, the output is invalid."),
                        new ClientMessage("user", prompt)))
                .temperature(temp)
                .top_p(1.0f)
                .max_completion_tokens(8192L)
                .stream(false)
                .extraParams(new HashMap<>())
                .build();
        if (model.equals("qwen/qwen3-32b")) {
            request.addExtraParam("reasoning", false);
            request.addExtraParam("include_reasoning", false);
            request.addExtraParam("response_format", Map.of("type", "json_object"));
        }
        return request;
    }
}
