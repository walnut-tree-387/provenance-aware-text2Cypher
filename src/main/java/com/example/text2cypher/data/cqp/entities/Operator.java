package com.example.text2cypher.data.cqp.entities;

import lombok.Getter;

@Getter
public enum Operator {
    EQ("="), GT(">"), LT("<"), BETWEEN("BETWEEN"), IN("IN"), NOT_IN("NOT IN");
    private final String value;

    Operator(String value) {
        this.value = value;
    }

}
