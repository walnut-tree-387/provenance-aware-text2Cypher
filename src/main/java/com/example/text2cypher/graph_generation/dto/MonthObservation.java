package com.example.text2cypher.graph_generation.dto;

import lombok.Data;

import java.util.List;

@Data
public class MonthObservation {
    private String month;
    private List<List<Long>> monthObservations;
}
