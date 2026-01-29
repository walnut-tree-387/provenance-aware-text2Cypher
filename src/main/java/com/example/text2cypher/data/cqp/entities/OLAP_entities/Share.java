package com.example.text2cypher.data.cqp.entities.OLAP_entities;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public final class Share implements PostAggregation {
    private String part;
    private String total;
    private String alias;

    public String toCypherExpression() {
        return "1.0 * " + part + " / " + total;
    }

    @Override
    public String alias() {
        return alias;
    }
}
