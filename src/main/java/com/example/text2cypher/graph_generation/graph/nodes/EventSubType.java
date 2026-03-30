package com.example.text2cypher.graph_generation.graph.nodes;

import lombok.Data;

import java.util.Map;

@Data
public class EventSubType {
    private String name;
    private Long severity;
    public void set(String name){
        this.name = name;
        this.severity = SEVERITY_MAP.getOrDefault(this.name, 0L);
    }

    public static final Map<String, Long> SEVERITY_MAP = Map.ofEntries(
            // Extreme / lethal
            Map.entry("murder", 5L),

            // Organized / armed
            Map.entry("dacoity", 4L),
            Map.entry("arms_act", 4L),

            // Violent
            Map.entry("robbery", 3L),
            Map.entry("explosive_act", 3L),
            Map.entry("narcotics", 3L),

            // Medium / disruptive
            Map.entry("riot", 2L),
            Map.entry("kidnapping", 2L),
            Map.entry("police_assault", 2L),
            Map.entry("women_and_child_repression", 2L),
            Map.entry("theft", 2L),
            Map.entry("burglary", 2L),
            Map.entry("smuggling", 2L),

            // Administrative / legal
            Map.entry("speedy_trial", 0L),
            Map.entry("other_cases", 0L)
    );

}
