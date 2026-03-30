package com.example.text2cypher.ais_evaluation.record;

import com.example.text2cypher.cypher_benchmark.dto.QueryType;

import java.util.List;

public interface IEvaluationService {
    List<EvaluationRecord> findAllByQueryType(QueryType queryType);
}
