package com.example.text2cypher.result;

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

    public AccuracyCalculator(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }
    public Map<String, List<Map<String, Double>>> calculate(QueryType queryType){
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
}
