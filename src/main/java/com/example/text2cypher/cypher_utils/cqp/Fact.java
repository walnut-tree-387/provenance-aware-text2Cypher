package com.example.text2cypher.cypher_utils.cqp;

public record Fact(String label, String field) {
    public static Fact OBSERVATION_COUNT =
            new Fact("Observation", "count");
}

