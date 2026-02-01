package com.example.text2cypher.data.cqp.entities.AIS.compilers;

import com.example.text2cypher.data.cqp.entities.OLAP_entities.Dimension;
import com.example.text2cypher.data.cqp.entities.OLAP_entities.Measure;
import com.example.text2cypher.data.cqp.entities.OLAP_entities.PostAggregation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
@Component
public class CompilerContext {
    private final Map<String, Dimension> axisSymbols = new HashMap<>();
    private final Map<String, Measure> measureSymbols = new HashMap<>();
    private final Map<String, PostAggregation> postAggregationSymbols = new HashMap<>();

    public void registerAxis(String alias, Dimension dimension) {
        if (axisSymbols.containsKey(alias)) {
            throw new RuntimeException("Duplicate axis alias: " + alias);
        }
        axisSymbols.put(alias, dimension);
    }

    public Dimension resolveAxis(String alias) {
        Dimension dim = axisSymbols.get(alias);
        if (dim == null) {
            throw new RuntimeException("Unknown axis alias: " + alias);
        }
        return dim;
    }
    public void registerMeasure(String alias, Measure measure) {
        if (measureSymbols.containsKey(alias)) {
            throw new RuntimeException("Duplicate measure alias: " + alias);
        }
        measureSymbols.put(alias, measure);
    }

    public Measure resolveMeasure(String alias) {
        Measure measure = measureSymbols.get(alias);
        if (measure == null) {
            throw new RuntimeException("Unknown axis alias: " + alias);
        }
        return measure;
    }
    public void registerPostAggregation(String alias, PostAggregation postAggregation) {
        if (postAggregationSymbols.containsKey(alias)) {
            throw new RuntimeException("Duplicate measure alias: " + alias);
        }
        postAggregationSymbols.put(alias, postAggregation);
    }

    public PostAggregation resolverPostAggregation(String alias) {
        PostAggregation postAggregation = postAggregationSymbols.get(alias);
        if (postAggregation == null) {
            throw new RuntimeException("Unknown Post Aggregation alias: " + alias);
        }
        return postAggregation;
    }
    public boolean hasAxis(String alias) {
        return axisSymbols.containsKey(alias);
    }

    public boolean hasMeasure(String alias) {
        return measureSymbols.containsKey(alias);
    }

    public boolean hasPostAggregation(String alias) {
        return postAggregationSymbols.containsKey(alias);
    }

    public boolean hasAny(String alias) {
        return hasAxis(alias) || hasMeasure(alias) || hasPostAggregation(alias);
    }
    public void clearContext() {
        axisSymbols.clear();
        measureSymbols.clear();
        postAggregationSymbols.clear();
    }
}
