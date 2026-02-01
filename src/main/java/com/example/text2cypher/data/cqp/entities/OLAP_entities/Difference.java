package com.example.text2cypher.data.cqp.entities.OLAP_entities;

import com.example.text2cypher.data.dto.PostAggregationType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public final class Difference implements PostAggregation {
    private String left;
    private String right;
    private String name;
    @Override
    public String name() {
        return name;
    }

    @Override
    public PostAggregationType getType() {
        return PostAggregationType.DIFFERENCE;
    }

    @Override
    public List<String> getOperands() {
        return List.of(left, right);
    }
}

