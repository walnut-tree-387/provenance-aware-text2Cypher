package com.example.text2cypher.cypher_utils.cqp;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public final class Difference implements PostAggregation {
    private String left;
    private String right;
    private String name;
    @Override
    public String getName() {
        return name;
    }

    @Override
    public PostAggregationType getType() {
        return PostAggregationType.DIFFERENCE;
    }

    @Override
    public List<String> getOperands() {
        return List.of(left, right);
    }
}

