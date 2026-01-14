package com.example.text2cypher.data.cypher;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QueryResultItem {
    private String value;
    private Long count;
}
