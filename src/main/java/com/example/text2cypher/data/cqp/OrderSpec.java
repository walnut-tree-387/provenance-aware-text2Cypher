package com.example.text2cypher.data.cqp;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderSpec {
    private String field;
    private OrderDirection direction;
    private OrderType orderType;
}
