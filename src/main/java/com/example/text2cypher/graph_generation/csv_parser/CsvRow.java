package com.example.text2cypher.graph_generation.csv_parser;

import jakarta.persistence.*;
import lombok.Data;
@Entity
@Table(name ="crime_csv")
@Data
public class CsvRow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String observationId;
    private String zone;
    private String month;
    private String division;
    private String type;
    private String subType;
    private Long count;
    private Long subTypeSeverity;
}
