package com.example.text2cypher.data.cypher;

import com.example.text2cypher.data.cqp.entities.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private void appendReturn(StringBuilder sb, CanonicalQueryPlan cqp) {
        sb.append("RETURN ");

        List<String> items = cqp.getReturnClause().getClauseItems().stream()
                .map(i -> i.getExpression() + " AS " + i.getAlias())
                .toList();

        sb.append(String.join(", ", items)).append("\n");
    }

    private void appendWith(StringBuilder sb, CanonicalQueryPlan cqp) {
        WithClause wc = cqp.getWithClause();
        List<AggregationExpression> aggregates = new ArrayList<>();
        List<DerivedExpression> derived = new ArrayList<>();
        wc.getExpressions().forEach(expr -> {
            if (expr instanceof AggregationExpression a) {
                aggregates.add(a);
            } else if (expr instanceof DerivedExpression d) {
                derived.add(d);
            } else {
                throw new IllegalStateException(
                        "Unsupported WithExpression: " + expr.getClass()
                );
            }
        });
        sb.append("WITH ");

        List<String> firstParts = new ArrayList<>();

        // group by items
        wc.getGroupByItems().forEach(i ->
                firstParts.add(i.getExpression() + " AS " + i.getAlias())
        );

        // aggregation expressions
        aggregates.forEach(a -> {
            if (a.getConstraint() != null) {
                String condition = constraintToCypher(a.getConstraint());
                firstParts.add(
                        a.getAggregationType() +
                                "(CASE WHEN " + condition +
                                " THEN " + a.getExpression() +
                                " ELSE 0 END) AS " + a.getAlias()
                );

            } else {
                firstParts.add(
                        a.getAggregationType() +
                                "(" + a.getExpression() +
                                ") AS " + a.getAlias()
                );
            }
        });
        // provenance ONLY in first WITH
        if (wc.isCollectProvenance()) {
            firstParts.add("collect(o) AS provenance");
        }
        sb.append(String.join(", ", firstParts)).append("\n");

        if (!derived.isEmpty()) {

            sb.append("WITH ");

            List<String> secondParts = new ArrayList<>();

            // carry forward all aggregate aliases
            aggregates.forEach(a ->
                    secondParts.add(a.getAlias())
            );

            // carry provenance
            if (wc.isCollectProvenance()) {
                secondParts.add("provenance");
            }

            // derived expressions
            derived.forEach(d ->
                    secondParts.add(d.getExpression() + " AS " + d.getAlias())
            );

            sb.append(String.join(", ", secondParts)).append("\n");
        }

    }
    private void appendOrderBy(StringBuilder sb, CanonicalQueryPlan cqp) {
        if (cqp.getOrderByClause() == null) return;

        sb.append("ORDER BY ")
                .append(cqp.getOrderByClause().getField())
                .append(" ")
                .append(cqp.getOrderByClause().getDirection())
                .append("\n").append("LIMIT ").append(cqp.getLimit().toString()).append(" \n");
    }
    private String constraintToCypher(Constraint c) {
        Object value = c.getValue();
        String formattedValue;
        if(c.getKey().equals("m.code") || c.getKey().equals("z.name") || c.getKey().equals("et.name") || c.getKey().equals("est.name")) {
            if (value instanceof String) {
                formattedValue = "'" + value + "'";
            } else {
                formattedValue = value.toString();
            }
            return c.getKey() + c.getOperator().getValue() + formattedValue;
        }
        else return c.getKey() + c.getOperator().getValue() + c.getValue();
    }

}
