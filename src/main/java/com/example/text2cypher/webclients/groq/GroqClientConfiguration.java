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

    @Value("${groq.api.key1}") private String k1;
    @Value("${groq.api.key2}") private String k2;
    @Value("${groq.api.key3}") private String k3;
    @Value("${groq.api.key4}") private String k4;
    @Value("${groq.api.key5}") private String k5;
    @Value("${groq.api.key6}") private String k6;
    @Value("${groq.api.key7}") private String k7;
    @Value("${groq.api.key8}") private String k8;

    @Value("${groq.api.timeout}")
    private int timeoutMs;

    @Bean
    public List<String> groqApiKeys() {
        return List.of(k1, k6, k4, k3, k2, k5, k7, k8);
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

