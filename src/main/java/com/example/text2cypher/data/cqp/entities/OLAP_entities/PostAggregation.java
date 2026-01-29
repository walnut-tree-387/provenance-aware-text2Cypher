package com.example.text2cypher.data.cqp.entities.OLAP_entities;

public sealed interface PostAggregation
        permits Ratio, Difference, Share {
    String toCypherExpression();
    String alias();
}
