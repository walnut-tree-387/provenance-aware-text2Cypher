package com.example.text2cypher.data.cqp.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class WithClause {
    private List<ClauseItem> groupByItems;
    private List<WithExpression> expressions;
    private boolean collectProvenance;
}
