package com.example.text2cypher.data.cqp.entities.OLAP_entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
public class CQP {
    private Fact fact;
    private List<Filter> filters;
    private List<GroupKey> groupBy;
    private List<Measure> measures;
    private List<Filter> provenanceFilters;
    private List<PostAggregation> postAggregations;
    private List<OrderSpec> orderClauses;
    private Integer limit;
    private Integer offset;
    private List<String> returnClauses;
}
