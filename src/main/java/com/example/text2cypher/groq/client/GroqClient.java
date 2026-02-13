package com.example.text2cypher.groq.client;

import com.example.text2cypher.groq.dto.GroqChatRequest;
import com.example.text2cypher.groq.dto.GroqChatResponse;
import com.example.text2cypher.groq.dto.GroqMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Random;

@Component
public class GroqClient {
    private final Random random = new Random();

    private final WebClient webClient;

    public GroqClient(WebClient groqWebClient) {
        this.webClient = groqWebClient;
    }

    public GroqChatResponse chatCompletion(String prompt, String model) {
        GroqChatRequest request = GroqChatRequest.builder()
                .model(model)
                .messages(List.of(
                        new GroqMessage("user", prompt)
                ))
                .temperature(random.nextFloat(0.7f, 1.0f))
                .build();

        try {
            return webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GroqChatResponse.class)
                    .block();
        } catch (Exception e) {
            System.err.println("Groq API call failed: " + e.getMessage() +
                    " for request: " + request + " with model: " + model + " and prompt: " + prompt);
            e.printStackTrace();
            return null;
        }

    }
}
