package com.example.text2cypher.data.cqp.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderByClause {
    private String field;      // total
    private String direction; // DESC
}