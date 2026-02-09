package com.example.text2cypher.groq.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GroqMessage {
    private String role;
    private String content;
}
