package com.example.text2cypher.data.cqp.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class WithClause {
    private List<ClauseItem> groupByItems;       // e.g. z.name AS key
    private List<AggregationExpression> aggregates; // sum(o.count) AS total
    private boolean collectProvenance;
}
