package com.example.text2cypher.data.dto;

public enum PostAggregationType {
    RATIO("/"), DIFFERENCE("-"), GT(">"), LT("<"), GTE(">="), LTE("<="), EQ("=");
    public final String value;

    PostAggregationType(String value) {
        this.value = value;
    }
}
