package com.example.text2cypher.data.dto;

import com.example.text2cypher.data.cqp.PostAggregationType;
import lombok.Data;

import java.util.Map;
@Data
public class PostAggregationDto {
    private PostAggregationType type;
    private Map<String, String> args;
    private String alias;
}
