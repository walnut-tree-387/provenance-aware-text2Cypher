package com.example.text2cypher.ais_evaluation.record;

import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntry;
import com.example.text2cypher.cypher_utils.cqp.CQP;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponse;
import com.example.text2cypher.utils.LocalMapper;
import org.springframework.stereotype.Service;

@Service
public class EvaluationService {
    private final EvaluationRecordRepository evaluationRecordRepository;
    public EvaluationService(EvaluationRecordRepository evaluationRecordRepository) {
        this.evaluationRecordRepository = evaluationRecordRepository;
    }

    public void create(String modelName, AIS ais, String question, GoldEntry gold, CQP predictedCQP, String predictedCypher, OlapCypherResponse result,
                       Long predicted, Long correct, Boolean executed, Boolean provenanceMatched){
        EvaluationRecord evaluationRecord = new EvaluationRecord();
        evaluationRecord.setModelName(modelName);
        evaluationRecord.setGoldEntry(gold);
        evaluationRecord.setQuestion(question);
        evaluationRecord.setPredictedAis(LocalMapper.write(ais));
        evaluationRecord.setPredictedCQP(LocalMapper.write(predictedCQP));
        evaluationRecord.setPredictedCypher(predictedCypher);
        if(result != null) evaluationRecord.setPredictedResult(LocalMapper.write(result.results()));
        if(result != null)evaluationRecord.setPredictedProvenance(LocalMapper.write(result.nodeList()));

        evaluationRecord.setExecuted(executed);
        evaluationRecord.setProvenanceMatched(provenanceMatched);
        evaluationRecord.setPredictedAttributes(predicted);
        evaluationRecord.setCorrectAttributes(correct);
        evaluationRecordRepository.save(evaluationRecord);
    }
}
