package com.example.text2cypher.cypher_utils.cqp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CQP {
    private Fact fact;
    private List<Filter> filters;
    private List<GroupKey> groupBy;
    private List<Measure> measures;
    private Map<Long, List<Map<Dimension, Set<String>>>> provenanceFilters;
    private List<PostAggregation> postAggregations;
    private List<OrderSpec> orderClauses;
    private Integer limit;
    private Integer offset;
    private List<String> returnClauses;
}
