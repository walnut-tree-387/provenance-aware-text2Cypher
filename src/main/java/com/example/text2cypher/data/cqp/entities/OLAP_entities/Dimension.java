package com.example.text2cypher.data.cqp.entities.OLAP_entities;

import lombok.Getter;

@Getter
public enum Dimension {
    ZONE_NAME("z.name"),
    ZONE_DIVISION("z.division"),
    MONTH_CODE("m.code"),
    MONTH_QUARTER("m.quarter"),
    MONTH_YEAR("m.year"),
    MONTH_MONTH("m.month"),
    EVENT_TYPE_NAME("et.name"),
    EVENT_SUBTYPE_NAME("est.name"),
    EVENT_SUBTYPE_SEVERITY("est.severity");
    private final String value;

    Dimension(String value) {
        this.value = value;
    }
}

