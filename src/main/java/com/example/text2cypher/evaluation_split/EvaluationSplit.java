package com.example.text2cypher.evaluation_split;

import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class EvaluationSplit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long gold_entry_id;
    private Long evaluation_stage;
    @Enumerated(EnumType.STRING)
    private QueryType queryType;
}
