package com.example.text2cypher.webclients.dto;

import lombok.Data;

@Data
public class OllamaChatResponse {
    private Message message;
    private String model;
    @Data
    public static class Message {
        private String role;
        private String content;
    }
}
