package com.example.text2cypher.data.cqp.entities.OLAP_entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CQP {
    private Fact fact;  // Observation.count

    // 2. Slice / dice
    private List<Filter> filters;

    // 3. Roll-up / drill-down
    private List<Dimension> groupBy;

    // 4. Aggregations
    private List<Measure> measures;

    // 5. Post-aggregation math (ratio, diff, share)
    private List<PostAggregation> postAggregations;

    // 6. Ordering (top-k)
    private OrderSpec order;

    // 7. Projection (what to return)
    private Projection projection;
}
