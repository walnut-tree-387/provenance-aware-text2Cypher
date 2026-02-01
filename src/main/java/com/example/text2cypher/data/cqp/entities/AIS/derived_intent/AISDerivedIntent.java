package com.example.text2cypher.data.cqp.entities.AIS.derived_intent;

import lombok.Data;

import java.util.List;
@Data
public class AISDerivedIntent {
    private String name;
    private AISDerivedType type;
    private List<String> operands;
}
