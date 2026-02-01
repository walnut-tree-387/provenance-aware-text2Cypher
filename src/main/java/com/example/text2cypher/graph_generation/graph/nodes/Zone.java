package com.example.text2cypher.graph_generation.graph.nodes;

import lombok.Data;

@Data
public class Zone {
    private String name;
    private String division;

    public void set(String name){
        this.name = name;
        switch (name){
            case "dmp" : case "dhaka_range" : case "gmp" : this.division = "Dhaka"; break;
            case "cmp" : case "chittagong_range" : this.division = "Chittagong"; break;
            case "kmp" : case "khulna_range" : this.division = "Khulna"; break;
            case "smp" : case "sylhet_range" : this.division = "Sylhet"; break;
            case "rmp" : case "rajshahi_range" : this.division = "Rajshahi"; break;
            case "bmp" : case "barishal_range" : this.division = "Barishal"; break;
            case "rpmp" : case "rangpur_range" : this.division = "Rangpur"; break;
            case "mymensingh_range" : this.division = "Mymensingh"; break;
            case "ralway_range" : this.division = "Railway"; break;
            default: break;
        }
    }
}
