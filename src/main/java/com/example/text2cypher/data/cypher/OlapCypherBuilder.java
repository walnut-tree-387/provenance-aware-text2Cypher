package com.example.text2cypher.data.cypher;
import com.example.text2cypher.data.cqp.entities.OLAP_entities.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class OlapCypherBuilder {
    public String build(CQP cqp) {
        StringBuilder cypher = new StringBuilder("""
            MATCH (o:Observation)
        """);
        matchClause(cypher);
        whereClause(cqp.getFilters(), cypher);
        aggregationWithClause(cqp.getGroupBy(), cqp.getMeasures(), cypher);
        postAggregationClause(cqp.getPostAggregations(), cypher);
        orderLimitClause(cqp.getOrder(), cypher);
        returnClause(cqp.getProjection(), cypher);
        return cypher.toString();
    }
    private void returnClause(Projection p, StringBuilder cypher) {
        cypher.append("RETURN ").append(String.join(", ", p.getFields())).append(", provenance");
    }

    private void orderLimitClause(OrderSpec o, StringBuilder cypher) {
        if (o == null) return;
        cypher.append( """
            ORDER BY %s %s
            LIMIT %d
        """.formatted(o.getField(), o.getDirection(), o.getLimit()));
    }
    private void postAggregationClause(List<PostAggregation> posts, StringBuilder cypher) {
        if (posts.isEmpty()) return;
        cypher.append("WITH *, ").append(posts.stream()
                .map(p -> p.toCypherExpression() + " AS " + p.alias())
                .collect(Collectors.joining(", "))).append("\n");
    }

    private void aggregationWithClause(List<Dimension> groupBy,
                                 List<Measure> measures, StringBuilder cypher) {
        List<String> groupExpressionList = groupBy.stream()
                .map(g -> Dimension.valueOf(String.valueOf(g)).getValue() + " AS " + g)
                .toList();
        List<String> measureExpressionList = measures.stream()
                .map(m -> (m.getFilters() != null && !m.getFilters().isEmpty()) ?
                        m.getAggregationType() + getAggregationFilters(m.getFilters()) + " AS " + m.getAlias()
                        :m.getAggregationType() + "(o.count) AS " + m.getAlias())
                .toList();
        cypher.append("WITH ").append(Stream.concat(groupExpressionList.stream(), measureExpressionList.stream())
                .collect(Collectors.joining(", "))).append(", collect(o) AS provenance").append("\n");
    }
    private String getAggregationFilters(List<Filter> filters) {
        List<String> conditions = new ArrayList<>();
        for(Filter filter : filters){
            conditions.add(filter.getDimension().getValue() + " " + filter.getOperator().getValue() + " " + filter.valueToCypher());
        }
        return "(CASE WHEN " + String.join(" AND ", conditions) + " THEN o.count ELSE 0 END)";
    }

    private void matchClause(StringBuilder cypher) {
        cypher.append("MATCH (o)-[:OF_SUBTYPE]->(est:EventSubType)-[:SUBTYPE_OF]->(et:EventType),\n");
        cypher.append("(o)-[:IN_MONTH]->(m:Month)\n");
        cypher.append(", (o)-[:OBSERVED_IN]->(z:Zone)\n");
    }
    private void whereClause(List<Filter> filters, StringBuilder cypher) {
        if (filters.isEmpty()) return;
        cypher.append("WHERE ").append(filters.stream()
                .map(f -> f.getDimension().getValue() + " " +
                        f.getOperator().getValue() + " " + f.valueToCypher())
                .collect(Collectors.joining(" AND "))).append("\n");
    }
}
