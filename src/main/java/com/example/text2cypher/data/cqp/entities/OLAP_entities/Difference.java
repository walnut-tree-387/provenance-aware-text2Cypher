package com.example.text2cypher.data.cqp.entities.OLAP_entities;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public final class Difference implements PostAggregation {
    private String left;
    private String right;
    private String alias;

    public String toCypherExpression() {
        return left + " - " + right;
    }

    @Override
    public String alias() {
        return alias;
    }
}

