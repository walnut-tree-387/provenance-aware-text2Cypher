package com.example.text2cypher.cypher_utils.cqp;

import lombok.Getter;

@Getter
public enum Dimension {
    ZONE_NAME("z.name"),
    ZONE_DIVISION("z.division"),
    MONTH_CODE("m.code"),
    MONTH_QUARTER("m.quarter"),
    MONTH_YEAR("m.year"),
    MONTH("m.month"),
    EVENT_TYPE("et.name"),
    EVENT_SUBTYPE("est.name"),
    EVENT_SUBTYPE_SEVERITY("est.severity"),
    OBSERVATION_COUNT("o.count");
    private final String value;

    Dimension(String value) {
        this.value = value;
    }
}

