package com.example.text2cypher.evaluation_split.few_shot_direct_cypher;

import com.example.text2cypher.ais_evaluation.record.FewShotCypherRecord;
import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntry;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponse;
import com.example.text2cypher.utils.LocalMapper;
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
        if(modelName.equals("gpt-oss:120b-cloud")) return "openai/gpt-oss-120b";
        else if (modelName.equals("kimi-k2:1t-cloud")) return "moonshotai/kimi-k2-instruct-0905";
        else if(modelName.equals("meta-llama/Llama-3.3-70B-Instruct:groq")) return "llama-3.3-70b-versatile";
        else if(modelName.equals("Qwen/Qwen3-32B:groq")) return "qwen/qwen3-32b";
        else return modelName;
    }
}
