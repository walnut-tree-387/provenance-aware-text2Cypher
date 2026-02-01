package com.example.text2cypher.graph_generation.graph.nodes;

import lombok.Data;

@Data
public class Month {
    private String code;
    private Long quarter;
    private Long month;
    private Long year;
    public void set(String code){
        this.code = code;
        String year = code.substring(0, 4);
        String month = code.substring(5, 7);
        this.year = Long.parseLong(year);
        this.month = Long.parseLong(month);
        this.quarter = (this.month % 3 == 0) ? this.month / 3 : (this.month / 3L) + 1L;
    }
}
