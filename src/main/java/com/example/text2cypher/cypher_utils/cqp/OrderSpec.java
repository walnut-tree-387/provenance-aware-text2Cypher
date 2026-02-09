package com.example.text2cypher.cypher_utils.cqp;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderSpec {
    private String field;
    private OrderDirection direction;
    private OrderType orderType;
}
