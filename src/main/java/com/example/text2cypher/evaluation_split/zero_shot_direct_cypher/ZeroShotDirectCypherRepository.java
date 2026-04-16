package com.example.text2cypher.evaluation_split.zero_shot_direct_cypher;

import com.example.text2cypher.ais_evaluation.record.CypherRecord;
import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.evaluation_split.few_shot_direct_cypher.FewShotDirectCypher;
import com.example.text2cypher.evaluation_split.zero_shot_ais2cypher.ZeroShotAis2Cypher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZeroShotDirectCypherRepository extends JpaRepository<ZeroShotDirectCypher, Long> {
    @Query("SELECT zsdc FROM ZeroShotDirectCypher zsdc WHERE zsdc.queryType = :queryType AND zsdc.evaluationStage = :evaluationStage")
    List<ZeroShotDirectCypher> findAllByQueryTypeAndEvaluationStage(QueryType queryType, Long evaluationStage);
    @Query("    SELECT zsdc from ZeroShotDirectCypher zsdc WHERE zsdc.predictedCypher IS NULL AND zsdc.modelName = :modelName ")
    List<ZeroShotDirectCypher> findAllByNullPredictedCypher(@Param("modelName") String modelName, Pageable pageable);
    @Query("    SELECT zsdc from ZeroShotDirectCypher zsdc WHERE zsdc.executed = FALSE ORDER BY zsdc.id DESC")
    List<ZeroShotDirectCypher> findAllByExecuted();
    List<ZeroShotDirectCypher> findAllByModelNameOrderById(String modelName);
}
