package com.example.text2cypher.paraphrasing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GroqMessage {
    private String role;
    private String content;
}
