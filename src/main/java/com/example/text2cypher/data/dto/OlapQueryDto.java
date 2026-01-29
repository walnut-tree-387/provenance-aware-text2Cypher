package com.example.text2cypher.data.dto;

import com.example.text2cypher.data.cqp.entities.OLAP_entities.*;
import lombok.Data;

import java.util.List;
@Data
public class OlapQueryDto {
    private List<Filter> filters;
    private List<Dimension> groupBy;
    private List<Measure> measures;
    private List<PostAggregationDto> postAggregations;
    private OrderSpec order;
    private List<String> projection;
}
