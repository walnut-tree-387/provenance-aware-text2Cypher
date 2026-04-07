package com.example.text2cypher.evaluation_split.zero_shot_ais2cypher;

import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntry;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class ZeroShotAis2Cypher {
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

    @Enumerated(EnumType.STRING)
    private QueryType queryType;

    private Long predictedAttributes;
    private Long correctAttributes;
    private Boolean executed = false;
    private Boolean resultMatch = false;
    private Boolean provenanceMatched = false;

    private String modelName;
    private Long evaluationStage;
}
