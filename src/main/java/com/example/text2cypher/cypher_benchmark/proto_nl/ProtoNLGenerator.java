package com.example.text2cypher.cypher_benchmark.proto_nl;
import com.example.text2cypher.cypher_utils.cqp.Filter;
import com.example.text2cypher.cypher_utils.cqp.GroupKey;
import com.example.text2cypher.cypher_utils.cqp.Measure;
import com.example.text2cypher.cypher_utils.cqp.OrderSpec;
import com.example.text2cypher.cypher_benchmark.dto.OlapQueryDto;
import com.example.text2cypher.cypher_benchmark.dto.PostAggregationDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProtoNLGenerator {

    public String generate(OlapQueryDto dto) {
        StringBuilder sb = new StringBuilder();
        appendGroupBy(sb, dto.getGroupBy());
        appendGlobalFilters(sb, dto.getFilters());
        appendMeasures(sb, dto.getMeasures());
        appendPostAggregations(sb, dto.getPostAggregations());
        appendOrder(sb, dto.getOrders());
        appendLimitOffset(sb, dto.getLimit(), dto.getOffset());
        appendReturn(sb, dto.getReturns());

        return sb.toString().trim();
    }
    private void appendGroupBy(StringBuilder sb, List<GroupKey> groupBy) {
        if (groupBy == null || groupBy.isEmpty()) return;
        for (GroupKey g : groupBy) {
            sb.append("Group the ")
                    .append(g.getDimension()).append(" AS ").append(g.getAlias());
            sb.append(",\n");
        }
    }
    private void appendGlobalFilters(StringBuilder sb, List<Filter> filters) {
        if (filters == null || filters.isEmpty()) return;
        sb.append("Apply Global filters ");
        for (Filter f : filters) {
                    sb.append(f.getDimension())
                    .append(" ")
                    .append(f.getOperator())
                    .append(" ")
                    .append(formatValue(f.getValue()))
                    .append(",\n");
        }
    }
    private void appendMeasures(StringBuilder sb, List<Measure> measures) {
        if (measures == null || measures.isEmpty()) return;
        sb.append("Calculate ");
        for (Measure m : measures) {
           sb.append(m.getAggregationType());
            if (m.getFilters() != null && !m.getFilters().isEmpty()) {
                sb.append(" WHERE ");
                sb.append(m.getFilters().stream()
                        .map(this::formatFilterInline)
                        .collect(Collectors.joining(" AND "))
                );
            }
            sb.append(" AS ").append(m.getAlias());
            sb.append(",\n");
        }
    }

    private void appendPostAggregations(StringBuilder sb, List<PostAggregationDto> postAggregations) {
        if (postAggregations == null || postAggregations.isEmpty()) return;
        sb.append("Apply post aggregations operations ");
        for (PostAggregationDto p : postAggregations) {
                    sb.append(p.getType())
                    .append(" to the operands ");
            List<String> args = p.getArgs();
            switch (p.getType()) {
                case RATIO ->
                        sb.append(args.get(0)).append(" / ").append(args.get(1));
                case DIFFERENCE ->
                        sb.append(args.get(0)).append(" - ").append(args.get(1));
                case EQ, GT, LT, GTE, LTE ->
                        sb.append(args.get(0)).append(" ")
                                .append(p.getType())
                                .append(" ")
                                .append(args.get(1));
                default ->
                        sb.append(String.join(", ", args));
            }
            sb.append(" AS ").append(p.getAlias());
            sb.append(",\n");
        }
    }
    private void appendOrder(StringBuilder sb, List<OrderSpec> orders) {
        if (orders == null || orders.isEmpty()) return;
        sb.append("Create an ordering by ");
        for (OrderSpec o : orders) {
                    sb.append(o.getField())
                    .append(" ")
                    .append(o.getDirection())
                    .append(",\n");
        }
    }
    private void appendLimitOffset(StringBuilder sb, Integer limit, Integer offset) {
        if (limit != null) {
            sb.append("Limit ").append(limit).append(",\n");
        }
        if (offset != null) {
            sb.append("skip ").append(offset).append(",\n");
        }
    }
    private void appendReturn(StringBuilder sb, List<String> returns) {
        if (returns == null || returns.isEmpty()) return;
        sb.append("return ");
        sb.append(String.join(", ", returns));
        sb.append(".\n");
    }
    private String formatFilterInline(Filter f) {
        return f.getDimension() + " " +
                f.getOperator().getValue() + " " +
                formatValue(f.getValue());
    }
    private String formatValue(Object value) {
        if (value == null) return "NULL";
        if (value instanceof List<?> list) {
            return "[" + list.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", ")) + "]";
        }
        return value.toString();
    }
}
