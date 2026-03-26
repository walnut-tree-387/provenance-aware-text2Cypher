package com.example.text2cypher.result;

import com.example.text2cypher.ais_evaluation.record.CypherRecord;
import com.example.text2cypher.ais_evaluation.record.CypherService;
import com.example.text2cypher.ais_evaluation.record.EvaluationRecord;
import com.example.text2cypher.ais_evaluation.record.EvaluationService;
import com.example.text2cypher.ais_evaluation.utils.ExactMatchAccuracy;
import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.cypher_utils.cqp.CQP;
import com.example.text2cypher.utils.LocalMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccuracyCalculator {
    private final EvaluationService evaluationService;
    private final CypherService cypherService;
    public AccuracyCalculator(EvaluationService evaluationService, CypherService cypherService) {
        this.evaluationService = evaluationService;
        this.cypherService = cypherService;
    }
    public Map<String, List<Map<String, Double>>> calculateAisAccuracy(QueryType queryType){
        List<EvaluationRecord> recordList = evaluationService.findAllByQueryType(queryType);
        if(recordList.size() % 4 != 0) throw new RuntimeException("Evaluation record size is not divisible by 4!");
        Map<String, long[]> map = new HashMap<>();
        for (EvaluationRecord record : recordList) {
            String modelName = record.getModelName();
            long predicted = record.getPredictedAttributes();
            long correct = record.getCorrectAttributes();
            String goldCqp = record.getGoldEntry().getGoldCqp();
            CQP goldCQP = LocalMapper.read(goldCqp, CQP.class);
            long gold = ExactMatchAccuracy.getPredictedScore(goldCQP);
            map.computeIfAbsent(modelName, k -> new long[6]);
            map.get(modelName)[0] += predicted;
            map.get(modelName)[1] += correct;
            map.get(modelName)[2] += gold;
            if(record.getExecuted())map.get(modelName)[3] += 1;
            if(record.getResultMatch())map.get(modelName)[4] += 1;
            if(record.getProvenanceMatched())map.get(modelName)[5] += 1;
        }
        Map<String, List<Map<String, Double>>> ans =  new HashMap<>();
        for(Map.Entry<String, long[]> entry : map.entrySet()){
            String modelName = entry.getKey();
            long totalEntry = recordList.size() / 4L;
            double recall = (double) entry.getValue()[1] / entry.getValue()[2];
            double precision = (double) entry.getValue()[1] / entry.getValue()[0];
            double f1 = (2 * recall * precision) / (recall + precision);
            double executionAccuracy = (double) map.get(modelName)[3] / totalEntry;
            double resultAccuracy = (double) map.get(modelName)[4] / totalEntry;
            double provenanceAccuracy = (double) map.get(modelName)[5] / totalEntry;
            Map<String, Double> recallMap = Map.of("recall", recall);
            Map<String, Double> precisionMap = Map.of("precision", precision);
            Map<String, Double> f1Map = Map.of("f1", f1);
            Map<String, Double> exe = Map.of("executionAccuracy", executionAccuracy);
            Map<String, Double> res = Map.of("resultAccuracy", resultAccuracy);
            Map<String, Double> prov = Map.of("provenanceAccuracy", provenanceAccuracy);
            ans.computeIfAbsent(modelName, k -> List.of(recallMap, precisionMap, f1Map, exe, res, prov));
        }
        return ans;
    }
    public Map<String, List<Map<String, Double>>> calculateCypherAccuracy(QueryType queryType){
        List<CypherRecord> recordList = cypherService.findAllByQueryType(queryType);
        if(recordList.size() % 2 != 0) throw new RuntimeException("Cypher record size is not divisible by 2!");
        Map<String, long[]> map = new HashMap<>();
        for (CypherRecord record : recordList) {
            String modelName = record.getModelName();
            map.computeIfAbsent(modelName, k -> new long[3]);
            if(record.getExecuted())map.get(modelName)[0] += 1;
            if(record.getResultMatch())map.get(modelName)[1] += 1;
            if(record.getProvenanceMatched())map.get(modelName)[2] += 1;
        }
        Map<String, List<Map<String, Double>>> ans =  new HashMap<>();
        for(Map.Entry<String, long[]> entry : map.entrySet()){
            String modelName = entry.getKey();
            long totalEntry = recordList.size() / 2L;
            double executionAccuracy = (double) map.get(modelName)[0] / totalEntry;
            double resultAccuracy = (double) map.get(modelName)[1] / totalEntry;
            double provenanceAccuracy = (double) map.get(modelName)[2] / totalEntry;
            Map<String, Double> exe = Map.of("executionAccuracy", executionAccuracy);
            Map<String, Double> res = Map.of("resultAccuracy", resultAccuracy);
            Map<String, Double> prov = Map.of("provenanceAccuracy", provenanceAccuracy);
            ans.computeIfAbsent(modelName, k -> List.of(exe, res, prov));
        }
        return ans;
    }
    public Map<QueryType, List<Map<String, Double>>> compareAccuracy(){
        Map<QueryType, List<Map<String, Double>> >ans =  new HashMap<>();
        for(QueryType queryType : QueryType.values()){
            Map<String, List<Map<String, Double>>> cypher = calculateCypherAccuracy(queryType);
            Map<String, List<Map<String, Double>>> ais = calculateAisAccuracy(queryType);
            double cypherEx1 = cypher.get("openai/gpt-oss-120b").get(0).get("executionAccuracy");
            double cypherEx2 = cypher.get("moonshotai/kimi-k2-instruct-0905").get(0).get("executionAccuracy");
            double cypherRes1 = cypher.get("openai/gpt-oss-120b").get(1).get("resultAccuracy");
            double cypherRes2 = cypher.get("moonshotai/kimi-k2-instruct-0905").get(1).get("resultAccuracy");
            double cypherProv1 = cypher.get("openai/gpt-oss-120b").get(2).get("provenanceAccuracy");
            double cypherProv2 = cypher.get("moonshotai/kimi-k2-instruct-0905").get(2).get("provenanceAccuracy");
            double aisEx1 = ais.get("openai/gpt-oss-120b").get(3).get("executionAccuracy");
            double aisEx2 = ais.get("moonshotai/kimi-k2-instruct-0905").get(3).get("executionAccuracy");
            double aisRes1 = ais.get("openai/gpt-oss-120b").get(4).get("resultAccuracy");
            double aisRes2 = ais.get("moonshotai/kimi-k2-instruct-0905").get(4).get("resultAccuracy");
            double aisProv1 = ais.get("openai/gpt-oss-120b").get(5).get("provenanceAccuracy");
            double aisProv2 = ais.get("moonshotai/kimi-k2-instruct-0905").get(5).get("provenanceAccuracy");
            Map<String, Double> cypherValues = new HashMap<>();
            cypherValues.put("cypher-executionAccuracy-gpt-oss-120b", cypherEx1);
            cypherValues.put("cypher-resultAccuracy-gpt-oss-120b", cypherRes1);
            cypherValues.put("cypher-provenanceAccuracy-gpt-oss-120b", cypherProv1);
            cypherValues.put("cypher-executionAccuracy-kimi-k2-instruct-0905", cypherEx2);
            cypherValues.put("cypher-resultAccuracy-kimi-k2-instruct-0905", cypherRes2);
            cypherValues.put("cypher-provenanceAccuracy-kimi-k2-instruct-0905", cypherProv2);
            Map<String, Double> aisValues = new HashMap<>();
            aisValues.put("ais-executionAccuracy-gpt-oss-120b", aisEx1);
            aisValues.put("ais-resultAccuracy-gpt-oss-120b", aisRes1);
            aisValues.put("ais-provenanceAccuracy-gpt-oss-120b", aisProv1);
            aisValues.put("ais-executionAccuracy-kimi-k2-instruct-0905", aisEx2);
            aisValues.put("ais-resultAccuracy-kimi-k2-instruct-0905", aisRes2);
            aisValues.put("ais-provenanceAccuracy-kimi-k2-instruct-0905", aisProv2);
            ans.computeIfAbsent(queryType, k -> List.of(aisValues, cypherValues));
        }
        return ans;
    }
}
