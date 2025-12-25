package com.example.text2cypher.data.cqp;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Constraint {
    private ConstraintLabel label;
    private String key;
    private Operator operator;
    private Object value;
}
