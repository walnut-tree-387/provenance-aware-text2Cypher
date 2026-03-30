package com.example.text2cypher.cypher_utils.cqp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;


@Data
@AllArgsConstructor
public final class Ratio implements PostAggregation{
    private String numerator;
    private String denominator;
    private String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public PostAggregationType getType() {
        return PostAggregationType.RATIO;
    }

    @JsonIgnore
    @Override
    public List<String> getCypherOperands() {
        return List.of("toFloat(" + numerator + ")", denominator);
    }
    @JsonIgnore
    @Override
    public List<String> getOparandList() {
        return List.of(numerator, denominator);
    }
}
