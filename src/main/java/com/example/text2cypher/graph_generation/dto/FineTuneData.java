package com.example.text2cypher.graph_generation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FineTuneData {
    private String answer;
    private String queryType;
    private String nl;
}
