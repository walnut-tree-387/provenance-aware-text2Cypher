package com.example.text2cypher.webclients.groq;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;

@Configuration
public class GroqClientConfiguration {
    @Value("${groq.api.url}")
    private String baseUrl;

    @Value("${groq.api.key}") private String key;

    @Value("${groq.api.timeout}")
    private int timeoutMs;

    @Bean
    public List<String> groqApiKeys() {
        return List.of(key);
    }

    @Bean
    public WebClient groqWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().responseTimeout(Duration.ofMillis(timeoutMs))))
                .build();
    }
}

