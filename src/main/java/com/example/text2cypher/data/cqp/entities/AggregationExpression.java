package com.example.text2cypher.data.cqp.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AggregationExpression {
    private AggregationType aggregationType;
    private String expression;
    private String alias;
}
