package com.example.text2cypher.ais_evaluation.record;

import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationRecordRepository extends JpaRepository<EvaluationRecord, Long> {

    List<EvaluationRecord> findAllByQueryType(QueryType queryType);
}
