package com.example.text2cypher.result;

import com.example.text2cypher.ais_evaluation.record.*;
import com.example.text2cypher.ais_evaluation.utils.ExactMatchAccuracy;
import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.cypher_utils.cqp.CQP;
import com.example.text2cypher.evaluation_split.few_shot_direct_cypher.FewShotDirectCypher;
import com.example.text2cypher.evaluation_split.few_shot_direct_cypher.FewShotDirectCypherService;
import com.example.text2cypher.evaluation_split.zero_shot_ais2cypher.ZeroShotAis2Cypher;
import com.example.text2cypher.evaluation_split.zero_shot_ais2cypher.ZeroShotAis2CypherService;
import com.example.text2cypher.evaluation_split.zero_shot_direct_cypher.ZeroShotDirectCypher;
import com.example.text2cypher.evaluation_split.zero_shot_direct_cypher.ZeroShotDirectCypherService;
import com.example.text2cypher.utils.LocalMapper;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccuracyCalculator {
    private final ZeroShotDirectCypherService directCypherService;
    private final FewShotDirectCypherService fewShotDirectCypherService;
    private final ZeroShotAis2CypherService zeroShotAis2CypherService;
    public AccuracyCalculator(ZeroShotDirectCypherService directCypherService,
                              FewShotDirectCypherService fewShotDirectCypherService, ZeroShotAis2CypherService zeroShotAis2CypherService) {
        this.directCypherService = directCypherService;
        this.fewShotDirectCypherService = fewShotDirectCypherService;
        this.zeroShotAis2CypherService = zeroShotAis2CypherService;
    }
    public Map<String, Map<String, Double>> calculateModelICSCapacity() {
        Map<String, MetricAccumulator> recallMap = new HashMap<>();
        Map<String, MetricAccumulator> precisionMap = new HashMap<>();
        Map<String, MetricAccumulator> f1Map = new HashMap<>();
        for (QueryType queryType : QueryType.values()) {
            for(long stage = 1L; stage <= 3L; stage++){
                List<ZeroShotAis2Cypher> recordList =
                        zeroShotAis2CypherService.findAllByQueryTypeAndEvaluationStage(queryType, stage);
                if (recordList.size() != 800)
                    throw new RuntimeException("Evaluation record size is not equal to 800");
                Map<String, long[]> temp = new HashMap<>();
                for (ZeroShotAis2Cypher record : recordList) {
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

                    recallMap.computeIfAbsent(model, k -> new MetricAccumulator()).add(recall);
                    precisionMap.computeIfAbsent(model, k -> new MetricAccumulator()).add(precision);
                    f1Map.computeIfAbsent(model, k -> new MetricAccumulator()).add(f1);
                }
                temp.clear();
            }
        }
        Map<String, Map<String, Double>> result = new HashMap<>();
        for (String model : recallMap.keySet()) {
            MetricAccumulator recallAcc = recallMap.get(model);
            MetricAccumulator precisionAcc = precisionMap.get(model);
            MetricAccumulator f1Acc = f1Map.get(model);
            double meanRecall = recallAcc.mean();
            double stdRecall = recallAcc.std(meanRecall);

            double meanPrecision = precisionAcc.mean();
            double stdPrecision = precisionAcc.std(meanPrecision);

            double meanF1 = f1Acc.mean();
            double stdF1 = f1Acc.std(meanF1);
            double ics = meanF1 * (1 - stdF1);

            Map<String, Double> metrics = new HashMap<>();
            metrics.put("recall_mean", meanRecall);
            metrics.put("recall_std", stdRecall);
            metrics.put("precision_mean", meanPrecision);
            metrics.put("precision_std", stdPrecision);
            metrics.put("f1_mean", meanF1);
            metrics.put("f1_std", stdF1);
            metrics.put("ics", ics);
            result.put(model, metrics);
        }

        return result;
    }
    public Map<String, Map<String, Double>> calculateQueryTypeAisAccuracy(QueryType queryType){
        Map<String, MetricAccumulator> execMap = new HashMap<>();
        Map<String, MetricAccumulator> resultMap = new HashMap<>();
        Map<String, MetricAccumulator> provMap = new HashMap<>();
        for (long stage = 1L; stage <= 3L; stage++) {
            List<ZeroShotAis2Cypher> recordList = zeroShotAis2CypherService.findAllByQueryTypeAndEvaluationStage(queryType, stage);
            if(recordList.size() != 800) throw new RuntimeException("throw new RuntimeException Evaluation record size is not equal to 800");
            Map<String, long[]> map = new HashMap<>();
            for (ZeroShotAis2Cypher record : recordList) {
                String modelName = record.getModelName();
                map.computeIfAbsent(modelName, k -> new long[3]);
                if(record.getExecuted())map.get(modelName)[0] += 1;
                if(record.getResultMatch())map.get(modelName)[1] += 1;
                if(record.getProvenanceMatched())map.get(modelName)[2] += 1;
            }
            for (Map.Entry<String, long[]> entry : map.entrySet()) {
                String model = entry.getKey();
                long[] v = entry.getValue();
                long totalEntry = recordList.size() / 4L;
                double exec = (double) v[0] / totalEntry;
                double res = (double) v[1] / totalEntry;
                double prov = (double) v[2] / totalEntry;
                execMap.computeIfAbsent(model, k -> new MetricAccumulator()).add(exec);
                resultMap.computeIfAbsent(model, k -> new MetricAccumulator()).add(res);
                provMap.computeIfAbsent(model, k -> new MetricAccumulator()).add(prov);
            }
            map.clear();
        }
        Map<String, Map<String, Double>> result = new HashMap<>();
        for (String model : execMap.keySet()) {
            Map<String, Double> metrics = new HashMap<>();
            double execMean = execMap.get(model).mean();
            metrics.put("execution_mean", execMean);
            metrics.put("execution_std", execMap.get(model).std(execMean));
            double resultMean = resultMap.get(model).mean();
            metrics.put("result_mean", resultMean);
            metrics.put("result_std", resultMap.get(model).std(resultMean));
            double provMean = provMap.get(model).mean();
            metrics.put("provenance_mean", provMean);
            metrics.put("provenance_std", provMap.get(model).std(provMean));
            result.put(model, metrics);
        }
        return result;
    }
    public Map<String, List<Map<String, Double>>> collectZeroShotCypherAccuracyData(QueryType queryType, Long evaluationStage){
        List<ZeroShotDirectCypher> recordList = directCypherService.findAllByQueryTypeAndStage(queryType, evaluationStage);
        if(recordList.size() != 800) throw new RuntimeException("Cypher record size should be equal to 800 for each queryType and evaluationStage");
        Map<String, long[]> map = new HashMap<>();
        for (ZeroShotDirectCypher record : recordList) {
            String modelName = record.getModelName();
            map.computeIfAbsent(modelName, k -> new long[3]);
            if(record.getExecuted())map.get(modelName)[0] += 1;
            if(record.getResultMatch())map.get(modelName)[1] += 1;
            if(record.getProvenanceMatched())map.get(modelName)[2] += 1;
        }
        Map<String, List<Map<String, Double>>> ans =  new HashMap<>();
        for(Map.Entry<String, long[]> entry : map.entrySet()){
            String modelName = entry.getKey();
            long totalEntry = recordList.size() / 4L;
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
    public Map<String, List<Map<String, Double>>> calculateFewShotCypherAccuracyData(QueryType queryType, Long evaluationStage){
        List<FewShotDirectCypher> recordList = fewShotDirectCypherService.findAllByQueryTypeAndEvaluationStage(queryType, evaluationStage);
        if(recordList.size() != 800) throw new RuntimeException("Cypher record size should be equal to 800 for each queryType and evaluationStage");
        Map<String, long[]> map = new HashMap<>();
        for (FewShotDirectCypher record : recordList) {
            String modelName = record.getModelName();
            map.computeIfAbsent(modelName, k -> new long[3]);
            if(record.getExecuted())map.get(modelName)[0] += 1;
            if(record.getResultMatch())map.get(modelName)[1] += 1;
            if(record.getProvenanceMatched())map.get(modelName)[2] += 1;
        }
        Map<String, List<Map<String, Double>>> ans =  new HashMap<>();
        for(Map.Entry<String, long[]> entry : map.entrySet()){
            String modelName = entry.getKey();
            long totalEntry = recordList.size() / 4L;
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
    public Map<String, List<Map<String, Double>>> calculateZeroShotAIS2CypherAccuracyData(QueryType queryType, Long stage) {
        List<ZeroShotAis2Cypher> recordList = zeroShotAis2CypherService.findAllByQueryTypeAndEvaluationStage(queryType, stage);
        if(recordList.size() != 800) throw new RuntimeException("Cypher record size should be equal to 800 for each queryType and evaluationStage");
        Map<String, long[]> map = new HashMap<>();
        for (ZeroShotAis2Cypher record : recordList) {
            String modelName = record.getModelName();
            map.computeIfAbsent(modelName, k -> new long[3]);
            if(record.getExecuted())map.get(modelName)[0] += 1;
            if(record.getResultMatch())map.get(modelName)[1] += 1;
            if(record.getProvenanceMatched())map.get(modelName)[2] += 1;
        }
        Map<String, List<Map<String, Double>>> ans =  new HashMap<>();
        for(Map.Entry<String, long[]> entry : map.entrySet()){
            String modelName = entry.getKey();
            long totalEntry = recordList.size() / 4L;
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
    public void calculateZeroShotAis2CypherAccuracy(String experimentName) {
        Map<String, Map<String, Map<Long, MetricAccumulator>>> store = new HashMap<>();
        for (long stage = 1L; stage <= 3L; stage++) {
            for (QueryType queryType : QueryType.values()) {
                Map<String, List<Map<String, Double>>> metrics;
                if(experimentName.equals("zero-shot-ais2cypher")) metrics = calculateZeroShotAIS2CypherAccuracyData(queryType, stage);
                else if(experimentName.equals("zero-shot-direct-cypher")) metrics = collectZeroShotCypherAccuracyData(queryType, stage);
                else metrics = calculateFewShotCypherAccuracyData(queryType, stage);
                store = updateStore(metrics, stage, store);
            }
        }
        System.out.println(" Experiment Name: " + experimentName + "\n");
        for (String model : store.keySet()) {
            System.out.println("\nModel: " + model);
            for (String metric : store.get(model).keySet()) {
                MetricAccumulator stageMeans = new MetricAccumulator();
                for (long stage : store.get(model).get(metric).keySet()) {
                    MetricAccumulator acc = store.get(model).get(metric).get(stage);
                    double stageMean = acc.mean();
                    double stageStd = acc.std(stageMean);
                    System.out.println("Stage " + stage + " | " + metric + " = " + round(stageMean) + " ± " + round(stageStd));
                    stageMeans.add(stageMean);
                }
                double finalMean = stageMeans.mean();
                double finalStd = stageMeans.std(finalMean);
                System.out.println("FINAL | " + metric + " = " + round(finalMean) + " ± " + round(finalStd));
            }
        }
    }
    private String round(double v) {
        return String.format("%.4f", v);
    }
    public Map<String, Map<String, Map<Long, MetricAccumulator>>> updateStore(Map<String, List<Map<String, Double>>> metrics, Long stage,
                                                                              Map<String, Map<String, Map<Long, MetricAccumulator>>> store){
        for (Map.Entry<String, List<Map<String, Double>>> entry : metrics.entrySet()) {
            String model = entry.getKey();
            List<Map<String, Double>> metricList = entry.getValue();
            double execution = metricList.get(0).get("executionAccuracy");
            double result = metricList.get(1).get("resultAccuracy");
            double provenance = metricList.get(2).get("provenanceAccuracy");
            store
                    .computeIfAbsent(model, k -> new HashMap<>())
                    .computeIfAbsent("execution", k -> new HashMap<>())
                    .computeIfAbsent(stage, k -> new MetricAccumulator())
                    .add(execution);

            store
                    .computeIfAbsent(model, k -> new HashMap<>())
                    .computeIfAbsent("result", k -> new HashMap<>())
                    .computeIfAbsent(stage, k -> new MetricAccumulator())
                    .add(result);

            store
                    .computeIfAbsent(model, k -> new HashMap<>())
                    .computeIfAbsent("provenance", k -> new HashMap<>())
                    .computeIfAbsent(stage, k -> new MetricAccumulator())
                    .add(provenance);
        }
        return store;
    }
}
