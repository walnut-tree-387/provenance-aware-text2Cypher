package com.example.text2cypher.data.cqp.entities.OLAP_entities;

import com.example.text2cypher.data.dto.PostAggregationType;
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
    public String name() {
        return name;
    }

    @Override
    public PostAggregationType getType() {
        return PostAggregationType.RATIO;
    }

    @Override
    public List<String> getOperands() {
        return List.of("toFloat(" + numerator + ")", denominator);
    }
}
