package com.example.text2cypher.webclients.huggingface;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;

@Configuration
public class HFConfiguration {
    @Value("${hf.api.url}")
    private String baseUrl;
    @Value("${groq.api.timeout}")
    private int timeoutMs;
    @Value("${hf.api.key}") private String key;
    @Bean
    public List<String> huggingFaceApiKeys() {
        return List.of(key);
    }

    @Bean
    public WebClient huggingFaceWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().responseTimeout(Duration.ofMillis(timeoutMs))))
                .build();
    }
}

