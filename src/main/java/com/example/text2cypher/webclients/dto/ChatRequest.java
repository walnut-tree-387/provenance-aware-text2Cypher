package com.example.text2cypher.webclients.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatRequest {
    private String model;
    private List<ClientMessage> messages;
    private double temperature;
    private double top_p;
    private Long max_completion_tokens;
    private boolean stream;
    private String reasoning_effort;
}
