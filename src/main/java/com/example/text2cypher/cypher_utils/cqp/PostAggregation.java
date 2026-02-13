package com.example.text2cypher.cypher_utils.cqp;

import java.util.List;

public sealed interface PostAggregation
        permits Ratio, Difference, Comparison {
    String getName();
    PostAggregationType getType();
    List<String> getOperands();
}
