package com.example.text2cypher.groq.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GroqChatRequest {
    private String model;
    private List<GroqMessage> messages;
    private double temperature;
}
