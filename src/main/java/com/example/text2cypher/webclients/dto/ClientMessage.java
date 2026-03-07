package com.example.text2cypher.webclients.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClientMessage {
    private String role;
    private String content;
}
