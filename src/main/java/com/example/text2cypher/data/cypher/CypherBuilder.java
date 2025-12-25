package com.example.text2cypher.data.cypher;

import com.example.text2cypher.data.cqp.CanonicalQueryPlan;
import com.example.text2cypher.data.cqp.Constraint;
import com.example.text2cypher.data.cqp.ConstraintLabel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CypherBuilder {

    public String build(CanonicalQueryPlan cqp) {

        StringBuilder sb = new StringBuilder("""
            MATCH (o:Observation)
        """);

        appendJoins(sb, cqp);
        appendWhere(sb, cqp);
        appendReturn(sb, cqp);

        return sb.toString();
    }

    private void appendJoins(StringBuilder sb, CanonicalQueryPlan cqp) {
        sb.append("MATCH (o)-[:OF_SUBTYPE]->(est:EventSubType)-[:SUBTYPE_OF]->(et:EventType),\n");
        sb.append("(o)-[:IN_MONTH]->(m:Month)\n");

        if (hasZoneConstraint(cqp)) {
            sb.append(", (o)-[:OBSERVED_IN]->(z:Zone)\n");
        }
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

    private void appendReturn(StringBuilder sb, CanonicalQueryPlan cqp) {
        sb.append("""
            RETURN sum(o.count) AS value,
                   collect(o) AS provenance
        """);
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
}
