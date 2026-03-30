package com.example.text2cypher.cypher_utils.cypher;
import com.example.text2cypher.cypher_utils.cqp.*;
import com.example.text2cypher.cypher_utils.cqp.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class OlapCypherBuilder {
    public String build(CQP cqp) {
        if(cqp == null) return "empty cypher for a rotten cqp";
        StringBuilder cypher = new StringBuilder("MATCH (o:Observation)\n");
        matchClause(cypher);
        whereClause(cqp.getFilters(), cypher);
        aggregationWithClause(cqp.getGroupBy(), cqp.getMeasures(), cypher);
        provenanceClause(cypher, cqp.getProvenanceFilters());
        postAggregationClause(cqp.getPostAggregations(), cypher);
        orderClause(cqp.getOrderClauses(), cypher);
        limitAndOffsetClause(cqp.getLimit(), cqp.getOffset(), cypher);
        returnClause(cqp.getReturnClauses(), cypher);
        return cypher.toString();
    }
    private void matchClause(StringBuilder cypher) {
        cypher.append("MATCH (o)-[:OF_SUBTYPE]->(est:EventSubType)-[:SUBTYPE_OF]->(et:EventType),\n");
        cypher.append("(o)-[:IN_MONTH]->(m:Month)\n");
        cypher.append(", (o)-[:OBSERVED_IN]->(z:Zone)\n");
    }
    private void returnClause(List<String> p, StringBuilder cypher) {
        cypher.append("RETURN ")
                .append(String.join(", ", p))
                .append(", provenance");
    }
    private void limitAndOffsetClause(Integer limit, Integer offset, StringBuilder cypher) {
        if(limit != null)cypher.append("LIMIT ").append(limit).append("\n");
        if(offset != null)cypher.append(" SKIP ").append(offset).append("\n");
    }
    private void orderClause(List<OrderSpec> o, StringBuilder cypher) {
        if(o.isEmpty()) return;
        cypher.append("ORDER BY ");
        String orderString = o.stream()
                .map(spec -> String.format("%s %s", spec.getField(), spec.getDirection()))
                .collect(Collectors.joining(", "));
        cypher.append(orderString).append("\n");

    }
    private void postAggregationClause(List<PostAggregation> posts, StringBuilder cypher) {
        if (posts.isEmpty()) return;
        List<List<PostAggregation>> layers = layerPostAggregations(posts);

        for (List<PostAggregation> layer : layers) {
            String calculations = layer.stream()
                    .map(p -> {
                        List<String> ops = p.getCypherOperands();
                        String op1 = ops.get(0);
                        String op2 = ops.get(1);
                        String name = p.getName();
                        String symbol = p.getType().value;
                        if (p.getType().equals(PostAggregationType.DIFFERENCE)) {
                            return String.format("abs(%s %s %s) AS %s", op1, symbol, op2, name);
                        } else if (p.getType().equals(PostAggregationType.RATIO)) {
                            return String.format("%s / CASE WHEN %s = 0 THEN null ELSE %s END AS %s", op1, op2, op2, name);
                        } else {
                            return String.format("%s %s %s AS %s", op1, symbol, op2, name);
                        }
                    })
                    .collect(Collectors.joining(", "));

            cypher.append("WITH *, ").append(calculations).append("\n");
        }
    }
    private void aggregationWithClause(List<GroupKey> groupBy,
                                       List<Measure> measures, StringBuilder cypher) {
        List<String> groupExpressionList = groupBy.stream()
                .map(g -> g.getDimension().getValue() + " AS " + g.getAlias())
                .toList();
        List<String> measureExpressionList = measures.stream()
                .map(m -> (m.getFilters() != null && !m.getFilters().isEmpty()) ?
                        getAggregationFilters(m.getFilters(), m.getAggregationType()) + " AS " + m.getAlias()
                        :m.getAggregationType().equals(AggregationType.COUNT_SUM) ? "SUM(o.count) AS " + m.getAlias() : "SUM(o.count * est.severity) AS " + m.getAlias())
                .toList();
        cypher.append("WITH ").append(Stream.concat(groupExpressionList.stream(), measureExpressionList.stream())
                .collect(Collectors.joining(", ")));
    }
    private void provenanceClause(StringBuilder cypher, Map<Long, List<Map<Dimension, Set<String>>>> provenanceFilters) {
        cypher.append(", collect(CASE \n" +
                "    WHEN o.count > 0 ");
        List<String> orParts = new ArrayList<>();
        for (Map.Entry<Long, List<Map<Dimension, Set<String>>>> entry : provenanceFilters.entrySet()) {
            StringJoiner andJoiner = new StringJoiner(" AND ", "(", ")");
            for (Map<Dimension, Set<String>> dimensionMap : entry.getValue()) {
                for (Dimension dimension : dimensionMap.keySet()) {
                    Set<String> vals = dimensionMap.get(dimension);
                    if (vals != null && !vals.isEmpty()) {
                        String clause = dimension.getValue() + " IN ['" + String.join("', '", vals) + "']";
                        andJoiner.add(clause);
                    }
                }
            }
            if (andJoiner.length() > 2) {
                orParts.add(andJoiner.toString());
            }
        }
        if(!orParts.isEmpty()) cypher.append("AND (").append(String.join(" OR ", orParts)).append(")");
        cypher.append(" THEN o END) AS provenance \n");
    }
    private String getAggregationFilters(List<Filter> filters, AggregationType aggregationType) {
        List<String> conditions = new ArrayList<>();
        for(Filter filter : filters){
            conditions.add(filter.getDimension().getValue() + " " + filter.getOperator().getValue() + " " + filter.valueToCypher());
        }
        String aggClause = "SUM(CASE WHEN " + String.join(" AND ", conditions) + " THEN ";
        aggClause += aggregationType.equals(AggregationType.WEIGHTED_SUM) ? "o.count * est.severity" : "o.count";
        aggClause += " ELSE 0 END)";
        return aggClause;
    }
    private void whereClause(List<Filter> filters, StringBuilder cypher) {
        if (filters.isEmpty()) return;
        cypher.append("WHERE ").append(filters.stream()
                .map(f -> {
                    String dim = f.getDimension().getValue();
                    String op = f.getOperator().getValue();
                    String val = f.valueToCypher();
                    if ("NOT IN".equalsIgnoreCase(op)) {
                        return "NOT " + dim + " IN " + val;
                    }
                    return dim + " " + op + " " + val;
                })
                .collect(Collectors.joining(" AND "))).append("\n");
    }
    private List<List<PostAggregation>> layerPostAggregations(List<PostAggregation> posts) {
        List<List<PostAggregation>> layers = new ArrayList<>();
        Set<String> resolved = new HashSet<>();
        Set<String> allPostAggNames = posts.stream()
                .map(PostAggregation::getName)
                .collect(Collectors.toSet());

        List<PostAggregation> remaining = new ArrayList<>(posts);

        while (!remaining.isEmpty()) {
            List<PostAggregation> layer = remaining.stream()
                    .filter(p -> p.getCypherOperands().stream().allMatch(op ->
                            !allPostAggNames.contains(op) || resolved.contains(op)
                    ))
                    .toList();

            if (layer.isEmpty()) {
                throw new RuntimeException("Circular or unresolved PostAggregation dependency");
            }

            layers.add(layer);
            layer.forEach(p -> resolved.add(p.getName()));
            remaining.removeAll(layer);
        }
        return layers;
    }

}
