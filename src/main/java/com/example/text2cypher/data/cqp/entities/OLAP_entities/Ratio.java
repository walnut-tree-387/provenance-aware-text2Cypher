package com.example.text2cypher.data.cqp.entities.OLAP_entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public final class Ratio implements PostAggregation{
    private String numeratorMeasure;
    private String denominatorMeasure;
    private String alias;

    @Override
    public String toCypherExpression() {
        return String.format(
                "CASE WHEN %s = 0 THEN 0.0 ELSE 100.0 * %s / %s END",
                denominatorMeasure, numeratorMeasure, denominatorMeasure
        );
    }

    @Override
    public String alias() {
        return alias;
    }
}
