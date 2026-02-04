package com.example.text2cypher.data.cqp;

public record Fact(String label, String field) {
    public static Fact OBSERVATION_COUNT =
            new Fact("Observation", "count");
}

