package com.example.text2cypher.cypher_benchmark.factories;

import com.example.text2cypher.cypher_utils.cqp.*;
import com.example.text2cypher.cypher_benchmark.dto.OlapQueryDto;
import com.example.text2cypher.cypher_benchmark.dto.PostAggregationDto;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class OlapCqpFactory {
    private final Map<Dimension, Set<Object>> convertedGlobalContext = new HashMap<>();
    public CQP fromDto(OlapQueryDto dto) {
        return new CQP(
                Fact.OBSERVATION_COUNT,
                compileFilters(dto),
                compileGroupBy(dto),
                compileMeasures(dto),
                compileProvenanceFilters(),
                compilePostAggregations(dto),
                compileOrder(dto),
                dto.getLimit(),
                dto.getOffset(),
                compileProjection(dto)
        );
    }
    private List<Filter> compileProvenanceFilters(){
        List<Filter> provenanceFilters = new ArrayList<>();
        for(Dimension key : convertedGlobalContext.keySet()) {
            Filter filter = new Filter(key, Operator.IN, convertedGlobalContext.get(key));
            provenanceFilters.add(filter);
        }
        convertedGlobalContext.clear();
        provenanceFilters.add(new Filter(Dimension.OBSERVATION_COUNT, Operator.GT, 0));
        return provenanceFilters;
    }
    private List<Filter> compileFilters(OlapQueryDto dto) {
        return (dto.getFilters() == null) ? List.of() : dto.getFilters();
    }
    private List<GroupKey> compileGroupBy(OlapQueryDto dto) {
        return dto.getGroupBy() == null ? List.of() : dto.getGroupBy();
    }
    private List<Measure> compileMeasures(OlapQueryDto dto) {
        return dto.getMeasures().stream()
                .map(m ->
                        new Measure(
                                m.getAggregationType(),
                                m.getAlias(),
                                m.getFilters()
                        )
                )
                .peek(m -> extractGlobalInContext(m.getFilters()))
                .toList();
    }
    private List<OrderSpec> compileOrder(OlapQueryDto dto) {
        return dto.getOrders().stream()
                .map(o -> new OrderSpec(o.getField(), o.getDirection(), o.getOrderType()))
                .toList();
    }
    private List<String> compileProjection(OlapQueryDto dto) {
        return dto.getReturns();
    }
    private List<PostAggregation> compilePostAggregations(OlapQueryDto dto) {
        if (dto.getPostAggregations() == null) return List.of();
        return dto.getPostAggregations().stream()
                        .map(this::compilePostAggregation)
                        .toList();

    }
    private PostAggregation compilePostAggregation(PostAggregationDto spec) {
        return switch (spec.getType()) {
            case RATIO -> new Ratio(
                    spec.getArgs().getFirst(),
                    spec.getArgs().getLast(),
                    spec.getAlias()
            );
            case DIFFERENCE -> new Difference(
                    spec.getArgs().getFirst(),
                    spec.getArgs().getLast(),
                    spec.getAlias()
            );
            default -> new Comparison(
                    spec.getAlias(),
                    spec.getType(),
                    spec.getArgs().getFirst(),
                    spec.getArgs().getLast()
            );
        };
    }
    private void extractGlobalInContext(List<Filter> filters) {
        if(filters == null) return;
        for (Filter filter : filters) {
            Dimension dimension = filter.getDimension();
            Operator operator = filter.getOperator();
            Object value = filter.getValue();
            if (operator == Operator.EQ) {
                convertedGlobalContext
                        .computeIfAbsent(dimension, d -> new HashSet<>())
                        .add(value);
            }
            else if (operator == Operator.IN) {
                convertedGlobalContext
                        .computeIfAbsent(dimension, d -> new HashSet<>())
                        .addAll((List<?>) value);
            }
        }
    }
}

