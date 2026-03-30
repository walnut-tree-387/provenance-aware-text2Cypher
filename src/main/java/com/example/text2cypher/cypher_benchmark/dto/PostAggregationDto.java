package com.example.text2cypher.cypher_benchmark.dto;

import com.example.text2cypher.cypher_utils.cqp.PostAggregationType;
import lombok.Data;

import java.util.List;

@Data
public class PostAggregationDto {
    private PostAggregationType type;
    private List<String> args;
    private String alias;
}
