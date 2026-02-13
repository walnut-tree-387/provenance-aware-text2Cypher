package com.example.text2cypher.ais_evaluation.record;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluationRecordRepository extends JpaRepository<EvaluationRecord, Long> {
}
