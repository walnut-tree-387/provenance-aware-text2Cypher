package com.example.text2cypher.cypher_benchmark.factories;

import com.example.text2cypher.ais_evaluation.utils.filter_utils.SemanticFilterNormalizer;
import com.example.text2cypher.cypher_utils.cqp.*;
import com.example.text2cypher.cypher_benchmark.dto.OlapQueryDto;
import com.example.text2cypher.cypher_benchmark.dto.PostAggregationDto;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class OlapCqpFactory {
    private final Map<Measure, Map<Dimension, Set<String>>> effectiveFiltersByMeasure = new HashMap<>();
    private final List<Filter> globalFilters = new ArrayList<>();
    private final SemanticFilterNormalizer filterNormalizer;

    public OlapCqpFactory(SemanticFilterNormalizer filterNormalizer) {
        this.filterNormalizer = filterNormalizer;
    }

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
    private Map<Long, List<Map<Dimension, Set<String>>>> compileProvenanceFilters(){
        Map<Dimension, Set<String>> normalizedGlobalFilters = filterNormalizer.normalize(globalFilters);
        Map<Long, List<Map<Dimension, Set<String>>>> provenanceFilters = new HashMap<>();
        Long idx = 0L;
        for(Measure m: effectiveFiltersByMeasure.keySet()){
            Map<Dimension, Set<String>> normalizedEffectiveFilters = effectiveFiltersByMeasure.get(m);
            List<Map<Dimension, Set<String>>> clauseList = new ArrayList<>();
            for(Dimension dimension : normalizedGlobalFilters.keySet()){
                Map<Dimension, Set<String>> clause = new HashMap<>();
                if(!Objects.equals(normalizedGlobalFilters.get(dimension), normalizedEffectiveFilters.get(dimension))){
                    clause.put(dimension, normalizedEffectiveFilters.get(dimension));
                }
                else clause.put(dimension, new HashSet<>());
                clauseList.add(clause);
            }
            provenanceFilters.put(idx, clauseList);
            idx++;
        }
        effectiveFiltersByMeasure.clear();
        globalFilters.clear();
        return provenanceFilters;
    }
    private List<Filter> compileFilters(OlapQueryDto dto) {
        globalFilters.addAll(dto.getFilters());
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
                .peek(this::storeNormalizedEffectiveFilters)
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
    private void storeNormalizedEffectiveFilters(Measure measure) {
        List<Filter> mergedFilters = new ArrayList<>(globalFilters);
        mergedFilters.addAll(measure.getFilters());
        Map<Dimension, Set<String>> normalized = filterNormalizer.normalize(mergedFilters);
        effectiveFiltersByMeasure.put(measure, normalized);
    }
}

