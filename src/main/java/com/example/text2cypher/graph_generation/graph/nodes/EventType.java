package com.example.text2cypher.graph_generation.graph.nodes;

import lombok.Data;

@Data
public class EventType {
    private String name;
    public void set(String name){
        this.name = name;
    }
}
