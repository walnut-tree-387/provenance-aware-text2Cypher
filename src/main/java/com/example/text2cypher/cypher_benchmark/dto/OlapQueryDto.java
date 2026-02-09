package com.example.text2cypher.cypher_benchmark.dto;

import com.example.text2cypher.cypher_utils.cqp.Filter;
import com.example.text2cypher.cypher_utils.cqp.GroupKey;
import com.example.text2cypher.cypher_utils.cqp.Measure;
import com.example.text2cypher.cypher_utils.cqp.OrderSpec;
import lombok.Data;

import java.util.List;
@Data
public class OlapQueryDto {
    private List<Filter> filters;
    private List<GroupKey> groupBy;
    private List<Measure> measures;
    private List<PostAggregationDto> postAggregations;
    private List<OrderSpec> orders;
    private List<String> returns;
    private Integer limit;
    private Integer offset;
    private QueryType queryType;
}
