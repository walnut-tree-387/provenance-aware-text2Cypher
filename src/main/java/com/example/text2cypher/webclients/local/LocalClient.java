package com.example.text2cypher.webclients.local;

import com.example.text2cypher.webclients.dto.ChatRequest;
import com.example.text2cypher.webclients.dto.ClientMessage;
import com.example.text2cypher.webclients.dto.OllamaChatResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
@Component
public class LocalClient {
    private final WebClient webClient;

    public LocalClient(WebClient localWebClient) {
        this.webClient = localWebClient;
    }
    public OllamaChatResponse chatCompletion(double temperature, String prompt, String model) {
        try {
            return webClient.post()
                    .uri("/api/chat")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(buildRequest(temperature, prompt, model))
                    .retrieve()
                    .bodyToMono(OllamaChatResponse.class)
                    .block();
        } catch (Exception e) {
            System.err.println("Error occurred during chat completion with local client: " + e.getMessage());
        }
        return null;
    }
    private ChatRequest buildRequest(double temp, String prompt, String model) {
        return ChatRequest.builder()
                .model(model)
                .messages(List.of(new ClientMessage("user", prompt)))
                .temperature(temp)
                .top_p(1.0f)
                .max_completion_tokens(8192L)
                .stream(false)
                .build();
    }
}
