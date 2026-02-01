package com.example.text2cypher.data.cqp.entities.OLAP_entities;

import com.example.text2cypher.data.dto.PostAggregationType;

import java.util.List;

public sealed interface PostAggregation
        permits Ratio, Difference, Comparison {
    String name();
    PostAggregationType getType();
    List<String> getOperands();
}
