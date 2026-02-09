package com.example.text2cypher.ais_evaluation.ais.derived_intent;

import lombok.Data;

import java.util.List;
@Data
public class AISDerivedIntent {
    private String name;
    private AISDerivedType type;
    private List<String> operands;
}
