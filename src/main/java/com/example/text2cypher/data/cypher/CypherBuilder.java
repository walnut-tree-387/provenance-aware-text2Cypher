package com.example.text2cypher.data.cypher;

import com.example.text2cypher.data.cqp.entities.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CypherBuilder {

    public String build(CanonicalQueryPlan cqp) {

        StringBuilder sb = new StringBuilder("""
            MATCH (o:Observation)
        """);

        appendJoins(sb, cqp);
        appendWhere(sb, cqp);
        appendWith( sb, cqp );
        appendOrderBy(sb, cqp);
        appendReturn(sb, cqp);

        return sb.toString();
    }
    private void appendJoins(StringBuilder sb, CanonicalQueryPlan cqp) {
        sb.append("MATCH (o)-[:OF_SUBTYPE]->(est:EventSubType)-[:SUBTYPE_OF]->(et:EventType),\n");
        sb.append("(o)-[:IN_MONTH]->(m:Month)\n");
        sb.append(", (o)-[:OBSERVED_IN]->(z:Zone)\n");
    }

    private void appendWhere(StringBuilder sb, CanonicalQueryPlan cqp) {
        List<String> clauses = new ArrayList<>();

        for (Constraint c : cqp.getConstraints()) {
            clauses.add(toWhereClause(c));
        }

        if (!clauses.isEmpty()) {
            sb.append("WHERE ")
                    .append(String.join(" AND ", clauses))
                    .append("\n");
        }
    }
    private String toWhereClause(Constraint c) {
        String var = switch (c.getLabel()) {
            case Zone -> "z";
            case EventType-> "et";
            case EventSubType -> "est";
            case Month -> "m";
            case Observation -> "o";
        };

        var = var + "." + c.getKey() + c.getOperator().getValue();
        var = (c.getValue() instanceof String) ? var + '"' +  c.getValue() + '"' : var + c.getValue();
        return var;
    }

    private boolean hasZoneConstraint(CanonicalQueryPlan cqp) {
        return cqp.getConstraints().stream()
                .anyMatch(c -> c.getLabel() == ConstraintLabel.Zone);
    }
    private void appendReturn(StringBuilder sb, CanonicalQueryPlan cqp) {
        sb.append("RETURN ");

        List<String> items = cqp.getReturnClause().getClauseItems().stream()
                .map(i -> i.getExpression() + " AS " + i.getAlias())
                .toList();

        sb.append(String.join(", ", items)).append("\n");
    }

    private void appendWith(StringBuilder sb, CanonicalQueryPlan cqp) {
        WithClause wc = cqp.getWithClause();
        if (wc == null) return;

        sb.append("WITH ");

        List<String> parts = new ArrayList<>();

        wc.getGroupByItems().forEach(i ->
                parts.add(i.getExpression() + " AS " + i.getAlias())
        );

        if(!wc.getAggregates().isEmpty())
        {
            wc.getAggregates().forEach(a ->
                    parts.add(a.getAggregationType() +
                            "(" + a.getExpression() + ") AS " + a.getAlias())
            );
        }

        if (wc.isCollectProvenance()) {
            parts.add("collect(o) AS provenance");
        }

        sb.append(String.join(", ", parts)).append("\n");
    }
    private void appendOrderBy(StringBuilder sb, CanonicalQueryPlan cqp) {
        if (cqp.getOrderByClause() == null) return;

        sb.append("ORDER BY ")
                .append(cqp.getOrderByClause().getField())
                .append(" ")
                .append(cqp.getOrderByClause().getDirection())
                .append("\n").append("LIMIT ").append(cqp.getLimit().toString()).append(" \n");
    }


}
