package com.example.text2cypher.cypher_utils.cqp;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Ratio.class, name = "RATIO"),
        @JsonSubTypes.Type(value = Difference.class, name = "DIFFERENCE"),

        @JsonSubTypes.Type(value = Comparison.class, name = "GT"),
        @JsonSubTypes.Type(value = Comparison.class, name = "LT"),
        @JsonSubTypes.Type(value = Comparison.class, name = "GTE"),
        @JsonSubTypes.Type(value = Comparison.class, name = "LTE"),
        @JsonSubTypes.Type(value = Comparison.class, name = "EQ")
})
public sealed interface PostAggregation
        permits Ratio, Difference, Comparison {
    String getName();
    PostAggregationType getType();
    List<String> getCypherOperands();
    List<String> getOparandList();
}
