package com.example.text2cypher.data.cqp.entities.OLAP_entities;

import com.example.text2cypher.data.cqp.entities.AggregationType;
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
