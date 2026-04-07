package com.example.text2cypher.evaluation_split.zero_shot_ais2cypher;

import com.example.text2cypher.ais_evaluation.record.EvaluationRecord;
import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.evaluation_split.zero_shot_direct_cypher.ZeroShotDirectCypher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZeroShotAis2CypherRepository extends JpaRepository<ZeroShotAis2Cypher, Long> {
    @Query("    SELECT zsac FROM ZeroShotAis2Cypher zsac WHERE zsac.queryType = :queryType AND zsac.evaluationStage = :evaluationStage")
    List<ZeroShotAis2Cypher> findAllByQueryTypeAndEvaluationStage(QueryType queryType, Long evaluationStage);
    @Query("    SELECT zsac from ZeroShotAis2Cypher zsac WHERE zsac.predictedAis = 'null' AND zsac.modelName = :modelName ")
    List<ZeroShotAis2Cypher> findAllByNullPredictedAis(@Param("modelName") String modelName, Pageable pageable);
}
