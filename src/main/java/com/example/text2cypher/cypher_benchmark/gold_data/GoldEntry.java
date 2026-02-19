package com.example.text2cypher.cypher_benchmark.gold_data;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class GoldEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String question;
    @Column(columnDefinition = "TEXT")
    private String goldCqp;
    @Column(columnDefinition = "TEXT")
    private String goldResult;
    @Column(columnDefinition = "TEXT")
    private String goldProvenance;
    @Column(columnDefinition = "TEXT")
    private String goldCypher;
    @Column(columnDefinition = "TEXT")
    private String protoNL;
    @Column(columnDefinition = "TEXT")
    private String modelName;
    private boolean processed = false;
}
