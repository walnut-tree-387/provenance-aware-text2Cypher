package com.example.text2cypher.data.cqp.entities;

import lombok.Getter;

@Getter
public enum Operator {
    EQ("="), GT(">"), LT("<"), BETWEEN("BETWEEN");
    private final String value;

    Operator(String value) {
        this.value = value;
    }

    Operator() {
        this.value = null;
    }

}
