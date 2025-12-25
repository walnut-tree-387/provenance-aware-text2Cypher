package com.example.text2cypher.graph_generation.csv_parser;

import lombok.Data;

@Data
public class CsvRow {
    private String zone;
    private String month;
    private String type;
    private String subType;
    private Long count;
}
