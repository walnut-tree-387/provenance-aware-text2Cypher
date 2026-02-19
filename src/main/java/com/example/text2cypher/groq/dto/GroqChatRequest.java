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
    private double top_p;
    private Long max_completion_tokens;
    private boolean stream;
    private String reasoning_effort;
}
