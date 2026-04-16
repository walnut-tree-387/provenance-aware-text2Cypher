package com.example.text2cypher.evaluation_split.few_shot_direct_cypher;

import com.example.text2cypher.ais_evaluation.record.FewShotCypherRecord;
import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntry;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponse;
import com.example.text2cypher.evaluation_split.zero_shot_direct_cypher.ZeroShotDirectCypher;
import com.example.text2cypher.utils.LocalMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FewShotDirectCypherService {
    private final FewShotDirectCypherRepository fewShotDirectCypherRepository;

    public FewShotDirectCypherService(FewShotDirectCypherRepository fewShotDirectCypherRepository) {
        this.fewShotDirectCypherRepository = fewShotDirectCypherRepository;
    }
    public List<FewShotDirectCypher> findAllByQueryTypeAndEvaluationStage(QueryType queryType, Long evaluationStage) {
        return fewShotDirectCypherRepository.findAllByQueryTypeAndEvaluationStage(queryType,  evaluationStage);
    }
    public void create(String modelName, String question, GoldEntry gold, String predictedCypher, OlapCypherResponse result,
                       Boolean executed, Boolean provenanceMatched, Boolean resultMatch, Long evaluationStage) {
        FewShotDirectCypher cypherRecord = new FewShotDirectCypher();
        cypherRecord.setModelName(getModelName(modelName));
        cypherRecord.setGoldEntry(gold);
        cypherRecord.setResultMatch(resultMatch);
        cypherRecord.setQuestion(question);
        cypherRecord.setQueryType(gold.getQueryType());
        cypherRecord.setPredictedCypher(predictedCypher);
        if(result != null) cypherRecord.setPredictedResult(LocalMapper.write(result.results()));
        if(result != null)cypherRecord.setPredictedProvenance(LocalMapper.write(result.nodeList()));

        cypherRecord.setExecuted(executed);
        cypherRecord.setProvenanceMatched(provenanceMatched);
        cypherRecord.setEvaluationStage(evaluationStage);
        fewShotDirectCypherRepository.save(cypherRecord);

    }
    public String getModelName(String modelName) {
        return switch (modelName) {
            case "gpt-oss:120b-cloud" -> "openai/gpt-oss-120b";
            case "kimi-k2:1t-cloud" -> "moonshotai/kimi-k2-instruct-0905";
            case "meta-llama/Llama-3.3-70B-Instruct:groq" -> "llama-3.3-70b-versatile";
            case "Qwen/Qwen3-32B:groq" -> "qwen/qwen3-32b";
            default -> modelName;
        };
    }
    public List<FewShotDirectCypher> getNullPredictedCypher(String modelName) {
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        PageRequest pageRequest = PageRequest.of(0, 15, sort);
        return fewShotDirectCypherRepository.findAllByNullPredictedCypher(modelName, pageRequest);
    }
    public void update(FewShotDirectCypher evaluationRecord, String predictedCypher, OlapCypherResponse result,
                       Boolean executed, Boolean provenanceMatched, Boolean resultMatch) {
        evaluationRecord.setResultMatch(resultMatch);
        evaluationRecord.setPredictedCypher(predictedCypher);
        if(result != null)evaluationRecord.setPredictedResult(LocalMapper.write(result.results()));
        if(result != null)evaluationRecord.setPredictedProvenance(LocalMapper.write(result.nodeList()));

        evaluationRecord.setExecuted(executed);
        evaluationRecord.setProvenanceMatched(provenanceMatched);
        fewShotDirectCypherRepository.save(evaluationRecord);
    }
    public List<FewShotDirectCypher> findAll(String modelName) {
        return fewShotDirectCypherRepository.findAllByModelNameOrderById(modelName);
    }
}
