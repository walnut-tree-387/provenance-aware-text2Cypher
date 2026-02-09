package com.example.text2cypher.cypher_utils.cqp;

import java.util.List;

public sealed interface PostAggregation
        permits Ratio, Difference, Comparison {
    String name();
    PostAggregationType getType();
    List<String> getOperands();
}
