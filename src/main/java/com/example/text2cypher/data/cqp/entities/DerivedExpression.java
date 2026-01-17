package com.example.text2cypher.data.cqp.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DerivedExpression implements WithExpression {
    private String expression;
    private String alias;
}
