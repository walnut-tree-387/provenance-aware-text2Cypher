package com.example.text2cypher.ais_evaluation.record;

import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntry;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class EvaluationRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "gold_entry_id")
    private GoldEntry goldEntry;
    @Column(columnDefinition = "TEXT")
    private String question;
    @Column(columnDefinition = "TEXT")
    private String predictedAis;
    @Column(columnDefinition = "TEXT")
    private String predictedCypher;
    @Column(columnDefinition = "TEXT")
    private String predictedCQP;
    @Column(columnDefinition = "TEXT")
    private String predictedResult;
    @Column(columnDefinition = "TEXT")
    private String predictedProvenance;

    private Long predictedAttributes;
    private Long correctAttributes;
    private boolean executed = false;
    private boolean provenanceMatched = false;

    private String modelName;
}
