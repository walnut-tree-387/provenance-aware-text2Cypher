package com.example.text2cypher.evaluation_split;

import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationSplitRepository extends JpaRepository<EvaluationSplit, Long> {
    @Query("""
        SELECT es
        FROM EvaluationSplit es
        WHERE es.queryType = :queryType AND es.evaluation_stage = :evaluationStage
    """)
    List<EvaluationSplit> findByQueryTypeAndEvaluationStage(
            @Param("queryType") QueryType queryType,
            @Param("evaluationStage") Long evaluationStage,
            Pageable pageable
    );
}
