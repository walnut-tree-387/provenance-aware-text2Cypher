package com.example.text2cypher.evaluation_split.few_shot_direct_cypher;

import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntry;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class FewShotDirectCypher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "gold_entry_id")
    private GoldEntry goldEntry;
    @Column(columnDefinition = "TEXT")
    private String question;
    @Column(columnDefinition = "TEXT")
    private String predictedCypher;
    @Column(columnDefinition = "TEXT")
    private String predictedResult;
    @Column(columnDefinition = "TEXT")
    private String predictedProvenance;

    @Enumerated(EnumType.STRING)
    private QueryType queryType;
    private Boolean executed = false;
    private Boolean resultMatch = false;
    private Boolean provenanceMatched = false;

    private String modelName;
    private Long evaluationStage;
}
