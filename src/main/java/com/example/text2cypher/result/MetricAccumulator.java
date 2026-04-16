package com.example.text2cypher.result;

import java.util.ArrayList;
import java.util.List;

public class MetricAccumulator {
    List<Double> values = new ArrayList<>();

    void add(double v) {
        values.add(v);
    }

    double mean() {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    double std(double mean) {
        return Math.sqrt(values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average().orElse(0));
    }
}