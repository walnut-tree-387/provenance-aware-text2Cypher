package com.example.text2cypher.graph_generation.graph.relations;

public enum RelationTypes {
    OBSERVED_IN, // observation to zone
    OF_SUBTYPE, // observation to EventSubType,
    IN_MONTH, // observation to month
    SUBTYPE_OF // EventSubType to EventType
}
