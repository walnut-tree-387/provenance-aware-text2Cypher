package com.example.text2cypher.data.cqp;

import lombok.Getter;

@Getter
public enum Operator {
    EQ("="), GT(">"), LT("<"), GTE(">="), LTE("<="), IN("IN"), NOT_IN("NOT IN");
    private final String value;

    Operator(String value) {
        this.value = value;
    }

}
