package com.example.text2cypher.ais_evaluation.record;

import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.ais_evaluation.utils.EvaluationScheduler;
import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntry;
import com.example.text2cypher.cypher_utils.cqp.CQP;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponse;
import com.example.text2cypher.utils.LocalMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EvaluationService implements IEvaluationService {

    private final EvaluationRecordRepository evaluationRecordRepository;
    public EvaluationService(EvaluationRecordRepository evaluationRecordRepository) {
        this.evaluationRecordRepository = evaluationRecordRepository;
    }
    @Override
    public List<EvaluationRecord> findAllByQueryType(QueryType queryType) {
        return evaluationRecordRepository.findAllByQueryType(queryType);
    }
    public void create(String modelName, AIS ais, String question, GoldEntry gold, CQP predictedCQP, String predictedCypher, OlapCypherResponse result,
                       Long predicted, Long correct, Boolean executed, Boolean provenanceMatched, Boolean resultMatch) {
        EvaluationRecord evaluationRecord = new EvaluationRecord();
        evaluationRecord.setModelName(getModelName(modelName));
        evaluationRecord.setGoldEntry(gold);
        evaluationRecord.setResultMatch(resultMatch);
        evaluationRecord.setQuestion(question);
        evaluationRecord.setQueryType(gold.getQueryType());
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
    public void update(EvaluationRecord evaluationRecord, AIS ais, CQP predictedCQP, String predictedCypher, OlapCypherResponse result,
                       Long predicted, Long correct, Boolean executed, Boolean provenanceMatched, Boolean resultMatch) {
        evaluationRecord.setResultMatch(resultMatch);
        evaluationRecord.setPredictedAis(LocalMapper.write(ais));
        evaluationRecord.setPredictedCQP(LocalMapper.write(predictedCQP));
        evaluationRecord.setPredictedCypher(predictedCypher);
        if(result != null)evaluationRecord.setPredictedResult(LocalMapper.write(result.results()));
        if(result != null)evaluationRecord.setPredictedProvenance(LocalMapper.write(result.nodeList()));

        evaluationRecord.setExecuted(executed);
        evaluationRecord.setProvenanceMatched(provenanceMatched);
        evaluationRecord.setPredictedAttributes(predicted);
        evaluationRecord.setCorrectAttributes(correct);
        evaluationRecordRepository.save(evaluationRecord);

    }
    public String getModelName(String modelName) {
        if(modelName.equals("gpt-oss:120b-cloud")) return "openai/gpt-oss-120b";
        else if (modelName.equals("kimi-k2:1t-cloud")) return "moonshotai/kimi-k2-instruct-0905";
        else if(modelName.equals("meta-llama/Llama-3.3-70B-Instruct:groq")) return "llama-3.3-70b-versatile";
        else if(modelName.equals("Qwen/Qwen3-32B:groq")) return "qwen/qwen3-32b";
        else return modelName;
    }

    public List<EvaluationRecord> getNullPredictedAIS(String modelName) {
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        PageRequest pageRequest = PageRequest.of(0, 20, sort);
        return evaluationRecordRepository.findAllByNullPredictedAis(modelName, pageRequest);
    }

}
