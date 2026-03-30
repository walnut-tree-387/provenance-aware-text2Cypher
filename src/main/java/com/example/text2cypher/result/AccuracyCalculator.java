package com.example.text2cypher.result;

import com.example.text2cypher.ais_evaluation.record.*;
import com.example.text2cypher.ais_evaluation.utils.ExactMatchAccuracy;
import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.cypher_utils.cqp.CQP;
import com.example.text2cypher.utils.LocalMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Service
public class AccuracyCalculator {
    private final EvaluationService evaluationService;
    private final CypherService cypherService;
    private final FewShotCypherService fewShotCypherService;
    public AccuracyCalculator(EvaluationService evaluationService, CypherService cypherService, FewShotCypherService fewShotCypherService) {
        this.evaluationService = evaluationService;
        this.cypherService = cypherService;
        this.fewShotCypherService = fewShotCypherService;
    }
    public Map<String, Map<String, Double>> calculateMeanAisPrediction() {
        Map<String, List<Double>> recallMap = new HashMap<>();
        Map<String, List<Double>> precisionMap = new HashMap<>();
        Map<String, List<Double>> f1Map = new HashMap<>();
        for (QueryType queryType : QueryType.values()) {
            List<EvaluationRecord> recordList =
                    evaluationService.findAllByQueryType(queryType);
            if (recordList.size() % 4 != 0)
                throw new RuntimeException("Evaluation record size is not divisible by 4!");
            Map<String, long[]> temp = new HashMap<>();
            for (EvaluationRecord record : recordList) {
                String modelName = record.getModelName();
                long predicted = record.getPredictedAttributes();
                long correct = record.getCorrectAttributes();
                String goldCqp = record.getGoldEntry().getGoldCqp();
                CQP goldCQP = LocalMapper.read(goldCqp, CQP.class);
                long gold = ExactMatchAccuracy.getPredictedScore(goldCQP);
                temp.computeIfAbsent(modelName, k -> new long[3]);
                temp.get(modelName)[0] += predicted; // predicted
                temp.get(modelName)[1] += correct;   // correct
                temp.get(modelName)[2] += gold;      // gold
            }
            for (Map.Entry<String, long[]> entry : temp.entrySet()) {
                String model = entry.getKey();
                long[] vals = entry.getValue();

                double precision = vals[0] == 0 ? 0 : (double) vals[1] / vals[0];
                double recall = vals[2] == 0 ? 0 : (double) vals[1] / vals[2];
                double f1 = (precision + recall == 0) ? 0 :
                        (2 * precision * recall) / (precision + recall);

                recallMap.computeIfAbsent(model, k -> new ArrayList<>()).add(recall);
                precisionMap.computeIfAbsent(model, k -> new ArrayList<>()).add(precision);
                f1Map.computeIfAbsent(model, k -> new ArrayList<>()).add(f1);
            }
        }

        // === Final Mean Calculation ===
        Map<String, Map<String, Double>> result = new HashMap<>();

        for (String model : recallMap.keySet()) {

            double meanRecall = recallMap.get(model).stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0);

            double meanPrecision = precisionMap.get(model).stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0);

            double meanF1 = f1Map.get(model).stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0);

            Map<String, Double> metrics = new HashMap<>();
            metrics.put("recall", meanRecall);
            metrics.put("precision", meanPrecision);
            metrics.put("f1", meanF1);

            result.put(model, metrics);
        }

        return result;
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
    public Map<String, List<Map<String, Double>>> calculateFewShotCypherAccuracy(QueryType queryType){
        List<FewShotCypherRecord> recordList = fewShotCypherService.findAllByQueryType(queryType);
        if(recordList.size() % 2 != 0) throw new RuntimeException("Cypher record size is not divisible by 2!");
        Map<String, long[]> map = new HashMap<>();
        for (FewShotCypherRecord record : recordList) {
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
            Map<String, List<Map<String, Double>>> fewShot = calculateFewShotCypherAccuracy(queryType);
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

            double fewEx1 = fewShot.get("openai/gpt-oss-120b").get(3).get("executionAccuracy");
            double fewEx2 = fewShot.get("moonshotai/kimi-k2-instruct-0905").get(3).get("executionAccuracy");
            double fewRes1 = fewShot.get("openai/gpt-oss-120b").get(4).get("resultAccuracy");
            double fewRes2 = fewShot.get("moonshotai/kimi-k2-instruct-0905").get(4).get("resultAccuracy");
            double fewProv1 = fewShot.get("openai/gpt-oss-120b").get(5).get("provenanceAccuracy");
            double fewProv2 = fewShot.get("moonshotai/kimi-k2-instruct-0905").get(5).get("provenanceAccuracy");
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

            Map<String, Double> fewshotValues = new HashMap<>();
            fewshotValues.put("fewshot-executionAccuracy-gpt-oss-120b", fewEx1);
            fewshotValues.put("fewshot-resultAccuracy-gpt-oss-120b", fewEx2);
            fewshotValues.put("fewshot-provenanceAccuracy-gpt-oss-120b", fewRes1);
            fewshotValues.put("fewshot-executionAccuracy-kimi-k2-instruct-0905", fewRes2);
            fewshotValues.put("fewshot-resultAccuracy-kimi-k2-instruct-0905", fewProv1);
            fewshotValues.put("fewshot-provenanceAccuracy-kimi-k2-instruct-0905", fewProv2);
            ans.computeIfAbsent(queryType, k -> List.of(aisValues, cypherValues, fewshotValues));
        }
        return ans;
    }
    public Map<String, Map<String, Double>> summarizeAccuracy() {
        Map<String, MetricAccumulator> acc = new HashMap<>();
        for (QueryType queryType : QueryType.values()) {
            Map<String, List<Map<String, Double>>> cypher = calculateCypherAccuracy(queryType);
            Map<String, List<Map<String, Double>>> ais = calculateAisAccuracy(queryType);
            Map<String, List<Map<String, Double>>> few = calculateFewShotCypherAccuracy(queryType);
            BiConsumer<String, Double> add = (key, value) -> {
                acc.computeIfAbsent(key, k -> new MetricAccumulator()).add(value);
            };

            // === AIS ===
            add.accept("ais-exec-gpt", ais.get("openai/gpt-oss-120b").get(3).get("executionAccuracy"));
            add.accept("ais-res-gpt", ais.get("openai/gpt-oss-120b").get(4).get("resultAccuracy"));
            add.accept("ais-prov-gpt", ais.get("openai/gpt-oss-120b").get(5).get("provenanceAccuracy"));

            add.accept("ais-exec-kimi", ais.get("moonshotai/kimi-k2-instruct-0905").get(3).get("executionAccuracy"));
            add.accept("ais-res-kimi", ais.get("moonshotai/kimi-k2-instruct-0905").get(4).get("resultAccuracy"));
            add.accept("ais-prov-kimi", ais.get("moonshotai/kimi-k2-instruct-0905").get(5).get("provenanceAccuracy"));

            // === CYPHER ZERO ===
            add.accept("cypher-exec-gpt", cypher.get("openai/gpt-oss-120b").get(0).get("executionAccuracy"));
            add.accept("cypher-res-gpt", cypher.get("openai/gpt-oss-120b").get(1).get("resultAccuracy"));
            add.accept("cypher-prov-gpt", cypher.get("openai/gpt-oss-120b").get(2).get("provenanceAccuracy"));

            add.accept("cypher-exec-kimi", cypher.get("moonshotai/kimi-k2-instruct-0905").get(0).get("executionAccuracy"));
            add.accept("cypher-res-kimi", cypher.get("moonshotai/kimi-k2-instruct-0905").get(1).get("resultAccuracy"));
            add.accept("cypher-prov-kimi", cypher.get("moonshotai/kimi-k2-instruct-0905").get(2).get("provenanceAccuracy"));

            // === FEW SHOT ===
            add.accept("few-exec-gpt", few.get("openai/gpt-oss-120b").get(0).get("executionAccuracy"));
            add.accept("few-res-gpt", few.get("openai/gpt-oss-120b").get(1).get("resultAccuracy"));
            add.accept("few-prov-gpt", few.get("openai/gpt-oss-120b").get(2).get("provenanceAccuracy"));

            add.accept("few-exec-kimi", few.get("moonshotai/kimi-k2-instruct-0905").get(0).get("executionAccuracy"));
            add.accept("few-res-kimi", few.get("moonshotai/kimi-k2-instruct-0905").get(1).get("resultAccuracy"));
            add.accept("few-prov-kimi", few.get("moonshotai/kimi-k2-instruct-0905").get(2).get("provenanceAccuracy"));
        }

        // === Final Output ===
        Map<String, Map<String, Double>> result = new HashMap<>();

        for (Map.Entry<String, MetricAccumulator> e : acc.entrySet()) {
            Map<String, Double> stats = new HashMap<>();
            stats.put("mean", e.getValue().mean());
            stats.put("std", e.getValue().std());
            result.put(e.getKey(), stats);
        }

        return result;
    }

}
