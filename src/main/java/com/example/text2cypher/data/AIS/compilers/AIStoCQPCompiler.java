package com.example.text2cypher.data.AIS.compilers;

import com.example.text2cypher.data.cqp.*;
import com.example.text2cypher.data.AIS.AIS;
import com.example.text2cypher.data.AIS.axes.AISAxis;
import com.example.text2cypher.data.AIS.context.AISContext;
import com.example.text2cypher.data.AIS.derived_intent.AISDerivedIntent;
import com.example.text2cypher.data.AIS.fact.AISFact;
import com.example.text2cypher.data.AIS.intent.AISIntent;
import com.example.text2cypher.data.AIS.order.AISOrderIntent;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
@Component
public class AIStoCQPCompiler {
    private final CompilerContext compilerContext;
    private final  Map<Dimension, Set<Object>> convertedGlobalContext = new HashMap<>();
    public AIStoCQPCompiler(CompilerContext compilerContext) {
        this.compilerContext = compilerContext;
    }
    public CQP mapToCQP(AIS ais) {
        if (ais == null) return null;
        CQP cqp = new CQP(
                compileFact(ais.getFact()),
                ais.getContext().stream()
                        .map(this::compileFilter)
                        .collect(Collectors.toList()),
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
    private List<Filter> setProvenanceFilters(){
        List<Filter> provenanceFilters = new ArrayList<>();
        for(Dimension key : convertedGlobalContext.keySet()) {
            Filter filter = new Filter(key, Operator.IN, convertedGlobalContext.get(key));
            provenanceFilters.add(filter);
        }
        convertedGlobalContext.clear();
        provenanceFilters.add(new Filter(Dimension.OBSERVATION_COUNT, Operator.GT, 0));
        return provenanceFilters;
    }

    private Fact compileFact(AISFact f) {
        return new Fact(f.getNode(), f.getField());
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
        extractGlobalInContext(filterList);
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
        if(operands.isEmpty()) return null;
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
                .peek(alias -> {
                    if (!compilerContext.hasAny(alias)) {
                        throw new RuntimeException("Unknown projection alias: " + alias);
                    }
                })
                .toList();
    }
    private void extractGlobalInContext(List<Filter> filters) {
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
