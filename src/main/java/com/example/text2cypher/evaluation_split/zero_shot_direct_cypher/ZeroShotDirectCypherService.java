package com.example.text2cypher.evaluation_split.zero_shot_direct_cypher;

import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.ais_evaluation.record.CypherRecord;
import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntry;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntryService;
import com.example.text2cypher.cypher_utils.cqp.CQP;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponse;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponseMapper;
import com.example.text2cypher.evaluation_split.few_shot_direct_cypher.FewShotDirectCypher;
import com.example.text2cypher.evaluation_split.zero_shot_ais2cypher.ZeroShotAis2Cypher;
import com.example.text2cypher.neo4j.Neo4jService;
import com.example.text2cypher.utils.LocalMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.example.text2cypher.ais_evaluation.utils.ProvenanceChecker.checkProvenanceForLLMGeneratedCypher;
import static com.example.text2cypher.ais_evaluation.utils.ProvenanceChecker.checkResult;

@Service
public class ZeroShotDirectCypherService {
    private final ZeroShotDirectCypherRepository zeroShotDirectCypherRepository;
    private final Neo4jService neo4jService;
    private final GoldEntryService goldEntryService;

    public ZeroShotDirectCypherService(ZeroShotDirectCypherRepository zeroShotDirectCypherRepository, Neo4jService neo4jService, GoldEntryService goldEntryService) {
        this.zeroShotDirectCypherRepository = zeroShotDirectCypherRepository;
        this.neo4jService = neo4jService;
        this.goldEntryService = goldEntryService;
    }

    public List<ZeroShotDirectCypher> findAllByQueryTypeAndStage(QueryType queryType, Long stage) {
        return zeroShotDirectCypherRepository.findAllByQueryTypeAndEvaluationStage(queryType, stage);
    }
    public void create(String modelName, String question, GoldEntry gold, String predictedCypher, OlapCypherResponse result,
                       Boolean executed, Boolean provenanceMatched, Boolean resultMatch, Long evaluationStage) {
        ZeroShotDirectCypher cypherRecord = new ZeroShotDirectCypher();
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
        zeroShotDirectCypherRepository.save(cypherRecord);
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
    public List<ZeroShotDirectCypher> getNullPredictedCypher(String modelName) {
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        PageRequest pageRequest = PageRequest.of(0, 10, sort);
        return zeroShotDirectCypherRepository.findAllByNullPredictedCypher(modelName, pageRequest);
    }
    public void update(ZeroShotDirectCypher evaluationRecord, String predictedCypher, OlapCypherResponse result,
                        Boolean executed, Boolean provenanceMatched, Boolean resultMatch) {
        evaluationRecord.setResultMatch(resultMatch);
        evaluationRecord.setPredictedCypher(predictedCypher);
        if(result != null)evaluationRecord.setPredictedResult(LocalMapper.write(result.results()));
        if(result != null)evaluationRecord.setPredictedProvenance(LocalMapper.write(result.nodeList()));

        evaluationRecord.setExecuted(executed);
        evaluationRecord.setProvenanceMatched(provenanceMatched);
        zeroShotDirectCypherRepository.save(evaluationRecord);
    }
//    @Scheduled(fixedDelay = 20 * 1000)
    public void processFailedCypher(){
        List<ZeroShotDirectCypher> failedCyphers = zeroShotDirectCypherRepository.findAllByExecuted();
        long changed = 0;
        for(ZeroShotDirectCypher failedCypher : failedCyphers){
            GoldEntry goldEntry = goldEntryService.findById(failedCypher.getGoldEntry().getId());
            String cypher =  failedCypher.getPredictedCypher();
            cypher = cleanEachQueryString(cypher);
            OlapCypherResponse testResponse;
            try{
                testResponse = OlapCypherResponseMapper.mapCypherResponse(neo4jService.fetch(cypher));
                boolean provenanceMatched = checkProvenanceForLLMGeneratedCypher(testResponse, goldEntry.getGoldProvenance());
                boolean resultMatch = checkResult(testResponse, goldEntry.getGoldResult());
                update(failedCypher, cypher, testResponse,
                        true, provenanceMatched, resultMatch);
                changed++;
            }catch (Exception e){
                System.out.println("Failed as well : " + failedCypher.getId());
                update(failedCypher, cypher, null,
                        false, false, false);
            }
        }
        System.out.println("changed: " + changed);
    }
    private String cleanEachQueryString(String cypher) {
        if (cypher == null) return null;
        return cypher
                .replace("\\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
    public List<ZeroShotDirectCypher> findAll(String modelName) {
        return zeroShotDirectCypherRepository.findAllByModelNameOrderById(modelName);
    }
}
