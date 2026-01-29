package com.example.text2cypher.data.cqp.entities.AIS.context;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AISContext {
    private AISDimension dimension;
    private AISOperator operator;
    private Object value;
}
