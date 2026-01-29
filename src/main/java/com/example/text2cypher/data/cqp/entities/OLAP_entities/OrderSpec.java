package com.example.text2cypher.data.cqp.entities.OLAP_entities;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderSpec {
    private String field;
    private OrderDirection direction;
    private Long limit;
}
