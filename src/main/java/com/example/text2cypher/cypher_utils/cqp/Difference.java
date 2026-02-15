package com.example.text2cypher.cypher_utils.cqp;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    public String getName() {
        return name;
    }

    @Override
    public PostAggregationType getType() {
        return PostAggregationType.DIFFERENCE;
    }

    @JsonIgnore
    @Override
    public List<String> getCypherOperands() {
        return List.of(left, right);
    }
    @JsonIgnore
    @Override
    public List<String> getOparandList() {
        return List.of(left, right);
    }
}

