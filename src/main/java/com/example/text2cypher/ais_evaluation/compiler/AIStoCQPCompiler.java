package com.example.text2cypher.ais_evaluation.compiler;

import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.ais_evaluation.ais.axes.AISAxis;
import com.example.text2cypher.ais_evaluation.ais.context.AISContext;
import com.example.text2cypher.ais_evaluation.ais.derived_intent.AISDerivedIntent;
import com.example.text2cypher.ais_evaluation.ais.fact.AISFact;
import com.example.text2cypher.ais_evaluation.ais.intent.AISIntent;
import com.example.text2cypher.ais_evaluation.ais.order.AISOrderIntent;
import com.example.text2cypher.ais_evaluation.utils.filter_utils.SemanticFilterNormalizer;
import com.example.text2cypher.cypher_utils.cqp.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
@Component
public class AIStoCQPCompiler {
    private final CompilerContext compilerContext;
    private final Map<Measure, Map<Dimension, Set<String>>> effectiveFiltersByMeasure = new HashMap<>();
    private final List<Filter> globalFilters = new ArrayList<>();
    private final SemanticFilterNormalizer filterNormalizer;
    public AIStoCQPCompiler(CompilerContext compilerContext, SemanticFilterNormalizer filterNormalizer) {
        this.compilerContext = compilerContext;
        this.filterNormalizer = filterNormalizer;
    }
    public CQP mapToCQP(AIS ais) {
        if (ais == null) return null;
        CQP cqp = new CQP(
                compileFact(ais.getFact()),
                compileFilters(ais.getContext()),
                ais.getAxes().stream()
                        .map(this::mapGroupKey)
                        .collect(Collectors.toList()),
                ais.getIntents().stream()
                        .map(this::mapMeasure)
                        .collect(Collectors.toList()),
                setProvenanceFilters(),
                ais.getDerivedIntents().stream()
                        .map(this::mapPostAggregation)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()),
                ais.getOrderIntents().stream()
                        .map(this::compileOrder)
                        .collect(Collectors.toList()),
                ais.getLimit(),
                ais.getOffset(),
                compileProjections(ais.getProjection())
        );
        compilerContext.clearContext();
        return cqp;
    }
    private List<Filter> compileFilters(List<AISContext> contexts){
        List<Filter> filters = contexts.stream()
                .map(this::compileFilter)
                .toList();
        globalFilters.addAll(filters);
        return filters;
    }
    private Map<Long, List<Map<Dimension, Set<String>>>> setProvenanceFilters(){
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

    private Fact compileFact(AISFact f) {
        return new Fact("Observation", "count");
    }
    private Filter compileFilter(AISContext c) {
        Dimension dimension = EnumCompiler.compileDimension(c.getDimension());
        Operator operator = EnumCompiler.compileOperator(c.getOperator());
        return new Filter(
                dimension, operator, c.getValue()
        );
    }
    private GroupKey mapGroupKey(AISAxis a) {
        String alias = normalizeAlias(a.getName());
        Dimension dimension = EnumCompiler.compileDimension(a.getDimension());
        compilerContext.registerAxis(alias, dimension);

        return new GroupKey(dimension, alias);
    }
    private Measure mapMeasure(AISIntent i) {
        String alias = normalizeAlias(i.getName());
        AggregationType type = EnumCompiler.compileIntentType(i.getType());
        List<Filter> filterList = i.getLocalContext().stream()
                .map(this::compileFilter)
                .toList();
        Measure measure = new Measure(type, alias, filterList);
        compilerContext.registerMeasure(alias, measure);
        storeNormalizedEffectiveFilters(measure);
        return measure;
    }
    private PostAggregation mapPostAggregation(AISDerivedIntent d) {
        PostAggregationType type = EnumCompiler.compileDerivedType(d.getType());
        List<String> operands = d.getOperands().stream().map(this::normalizeAlias).collect(Collectors.toList());
        String alias = normalizeAlias(d.getName());
        PostAggregation postAggregation = compilePostAggregation(type, operands, alias);
        compilerContext.registerPostAggregation(alias, postAggregation);
        return postAggregation;
    }

    private String normalizeAlias(String raw) {
        String normalized = raw.toLowerCase();
        if (normalized.matches("-?\\d+(\\.\\d+)?")) {
            return normalized;
        }
        return normalized.replaceAll("[^a-z0-9_]", "_");
    }
    private PostAggregation compilePostAggregation(PostAggregationType type, List<String> operands, String alias) {
        if(operands.size() != 2) return null;
        return switch (type) {
            case RATIO -> new Ratio(operands.get(0),  operands.get(1), alias);
            case DIFFERENCE -> new Difference(operands.get(0),  operands.get(1), alias);
            default -> new Comparison(alias, type, operands.get(0), operands.get(1));
        };
    }
    private OrderSpec compileOrder(AISOrderIntent orderIntent){
        String alias = normalizeAlias(orderIntent.getBy());
        OrderDirection direction = EnumCompiler.compileOrderDirection(orderIntent.getDirection());
        if(compilerContext.hasAxis(alias)){
            return new OrderSpec(alias, direction, OrderType.DIMENSION);
        }
        else if(compilerContext.hasMeasure(alias)){
            return new OrderSpec(alias, direction, OrderType.MEASURE);
        }
        else if(compilerContext.hasPostAggregation(alias)){
            return new OrderSpec(alias, direction, OrderType.POST_AGGREGATION);
        }
        return null;
    }
    private List<String> compileProjections(List<String> projections) {
        return projections.stream()
                .map(this::normalizeAlias)
                .filter(compilerContext :: hasAny)
                .toList();
    }
    private void storeNormalizedEffectiveFilters(Measure measure) {
        List<Filter> mergedFilters = new ArrayList<>(globalFilters);
        mergedFilters.addAll(measure.getFilters());
        Map<Dimension, Set<String>> normalized = filterNormalizer.normalize(mergedFilters);
        effectiveFiltersByMeasure.put(measure, normalized);
    }

}
