package com.example.text2cypher.evaluation_split.few_shot_direct_cypher;

import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.evaluation_split.zero_shot_direct_cypher.ZeroShotDirectCypher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FewShotDirectCypherRepository extends JpaRepository<FewShotDirectCypher,Long> {
    @Query("    SELECT fsdc FROM FewShotDirectCypher fsdc WHERE fsdc.queryType = :queryType AND fsdc.evaluationStage = :evaluationStage ")
    List<FewShotDirectCypher> findAllByQueryTypeAndEvaluationStage(QueryType queryType, Long evaluationStage);
    @Query("    SELECT fsdc from FewShotDirectCypher fsdc WHERE fsdc.predictedCypher IS NULL AND fsdc.modelName = :modelName ")
    List<FewShotDirectCypher> findAllByNullPredictedCypher(@Param("modelName") String modelName, Pageable pageable);
    List<FewShotDirectCypher> findAllByModelNameOrderById(String modelName);
}
