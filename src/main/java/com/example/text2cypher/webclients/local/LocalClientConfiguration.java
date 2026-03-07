package com.example.text2cypher.webclients.local;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
@Configuration
public class LocalClientConfiguration {
    @Value("${local.api.url}")
    private String baseUrl;
    @Value("${groq.api.timeout}")
    private int timeoutMs;

    @Bean
    public WebClient localWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()))
                .build();
    }
}
