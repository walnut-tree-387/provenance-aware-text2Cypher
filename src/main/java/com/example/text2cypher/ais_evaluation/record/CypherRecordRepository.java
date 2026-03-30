package com.example.text2cypher.ais_evaluation.record;

import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CypherRecordRepository extends JpaRepository<CypherRecord, Long> {
    List<CypherRecord> findAllByQueryType(QueryType queryType);
}
