package com.example.text2cypher.data.cqp;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Measure {
    private AggregationType aggregationType;
    private String alias;
    private List<Filter> filters;
}
