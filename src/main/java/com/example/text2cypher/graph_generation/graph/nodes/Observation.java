package com.example.text2cypher.graph_generation.graph.nodes;

import lombok.Data;

@Data
public class Observation {
    private String id;
    private Long count;
    private String source = "https://www.police.gov.bd/";

    public void set(Long count){
        this.count = count;
    }
}
