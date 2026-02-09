package com.example.text2cypher.cypher_benchmark.gold_data;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class GoldEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String question;
    @Column(columnDefinition = "TEXT")
    private String goldCqp;
    private String goldResult;
    @Column(columnDefinition = "TEXT")
    private String goldProvenance;
    @Column(columnDefinition = "TEXT")
    private String goldCypher;
    private boolean processed = false;
}
