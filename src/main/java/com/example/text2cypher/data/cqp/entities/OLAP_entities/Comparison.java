package com.example.text2cypher.data.cqp.entities.OLAP_entities;

import com.example.text2cypher.data.dto.PostAggregationType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public final class Comparison implements PostAggregation {
    private String name;
    private PostAggregationType type;
    private String left;
    private String right;

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<String> getOperands() {
        return List.of(left, right);
    }
}
