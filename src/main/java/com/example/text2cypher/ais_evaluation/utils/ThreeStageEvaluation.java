package com.example.text2cypher.ais_evaluation.utils;

import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.ais_evaluation.compiler.AIStoCQPCompiler;
import com.example.text2cypher.ais_evaluation.record.EvaluationRecord;
import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntry;
import com.example.text2cypher.cypher_utils.cqp.CQP;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherBuilder;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponse;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponseMapper;
import com.example.text2cypher.evaluation_split.EvaluationSplitService;
import com.example.text2cypher.evaluation_split.zero_shot_ais2cypher.ZeroShotAis2Cypher;
import com.example.text2cypher.evaluation_split.zero_shot_ais2cypher.ZeroShotAis2CypherService;
import com.example.text2cypher.neo4j.Neo4jService;
import com.example.text2cypher.utils.LocalMapper;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.example.text2cypher.ais_evaluation.utils.ProvenanceChecker.checkProvenance;
import static com.example.text2cypher.ais_evaluation.utils.ProvenanceChecker.checkResult;

@Service
public class ThreeStageEvaluation {
    private final EvaluationSplitService evaluationSplitService;
    private static final Long evaluationStage = 3L;
    private final AISGenerator aisGenerator;
    private final AIStoCQPCompiler cqpCompiler;
    private final OlapCypherBuilder cypherBuilder;
    private final Neo4jService neo4jService;
    private final ZeroShotAis2CypherService zeroShotAis2CypherService;

    public ThreeStageEvaluation(EvaluationSplitService evaluationSplitService, AISGenerator aisGenerator, AIStoCQPCompiler cqpCompiler, OlapCypherBuilder cypherBuilder, Neo4jService neo4jService, ZeroShotAis2CypherService zeroShotAis2CypherService) {
        this.evaluationSplitService = evaluationSplitService;
        this.aisGenerator = aisGenerator;
        this.cqpCompiler = cqpCompiler;
        this.cypherBuilder = cypherBuilder;
        this.neo4jService = neo4jService;
        this.zeroShotAis2CypherService = zeroShotAis2CypherService;
    }

    @Transactional(rollbackOn =  EvaluationRollbackException.class)
//    @Scheduled(fixedDelay = 20 * 1000)
    public void processZeroShotAISBatch(){
        List<GoldEntry> goldEntries = evaluationSplitService.findByQueryTypeAndEvaluationStage(QueryType.BOOLEAN, evaluationStage, 15);
        Map<String, List<AIS>> aisMap;
        try{
            aisMap = aisGenerator.generateAISBatch(goldEntries);
        } catch(Exception e){
            throw new EvaluationRollbackException("AIS generation failed");
        }
        for(int i = 0; i < goldEntries.size(); i++){
            GoldEntry goldEntry = goldEntries.get(i);
            CQP goldCQP = LocalMapper.read(goldEntry.getGoldCqp(), CQP.class);
            for(String key:  aisMap.keySet()){
                List<AIS> aisList = null;
                AIS ais = null;
                if(aisMap.get(key) != null)aisList = aisMap.get(key);
                if (aisList != null && !aisList.isEmpty() && i < aisList.size()) ais = aisList.get(i);
                CQP testCQP = cqpCompiler.mapToCQP(ais);
                Long predictedScore = ExactMatchAccuracy.getPredictedScore(testCQP);
                Long correctScore = ExactMatchAccuracy.getCorrectScore(goldCQP, testCQP);
                String testCypher = cypherBuilder.build(testCQP);
                OlapCypherResponse testResponse;
                try{
                    testResponse = OlapCypherResponseMapper.map(neo4jService.fetch(testCypher), testCQP.getReturnClauses());
                    boolean provenanceMatched = checkProvenance(testResponse, goldEntry.getGoldProvenance());
                    boolean resultMatch = checkResult(testResponse, goldEntry.getGoldResult());
                    zeroShotAis2CypherService.create(key, ais, goldEntry.getQuestion(), goldEntry, testCQP, testCypher,
                            testResponse, predictedScore, correctScore, true, provenanceMatched, resultMatch, evaluationStage);
                }catch (Exception e){
                    zeroShotAis2CypherService.create(key, ais, goldEntry.getQuestion(), goldEntry, testCQP, testCypher,
                            null, predictedScore, correctScore, false, false, false, evaluationStage);
                }
            }
        }
        System.out.println("Evaluation finished for " + goldEntries.getFirst().getId() + " to " + goldEntries.getLast().getId());
    }
//    @Scheduled(fixedDelay = 40 * 1000)
    public void updateNullZeroShotAISBatch(){
        String modelName = "llama-3.3-70b-versatile";
        List<ZeroShotAis2Cypher> nullPredicted = zeroShotAis2CypherService.getNullPredictedAIS(modelName);
        List<String> questions = nullPredicted.stream().map(ZeroShotAis2Cypher::getQuestion).toList();
        List<AIS> aisList = aisGenerator.generateAISBatchForNullPredictedAIS(questions, modelName);
        for(int i = 0; i < nullPredicted.size(); i++){
            AIS ais = null;
            if (aisList != null && !aisList.isEmpty() && i < aisList.size()) ais = aisList.get(i);
            GoldEntry goldEntry = nullPredicted.get(i).getGoldEntry();
            CQP goldCQP = LocalMapper.read(goldEntry.getGoldCqp(), CQP.class);
            CQP testCQP = cqpCompiler.mapToCQP(ais);
            Long predictedScore = ExactMatchAccuracy.getPredictedScore(testCQP);
            Long correctScore = ExactMatchAccuracy.getCorrectScore(goldCQP, testCQP);
            String testCypher = cypherBuilder.build(testCQP);
            OlapCypherResponse testResponse;
            try{
                testResponse = OlapCypherResponseMapper.map(neo4jService.fetch(testCypher), testCQP.getReturnClauses());
                boolean provenanceMatched = checkProvenance(testResponse, goldEntry.getGoldProvenance());
                boolean resultMatch = checkResult(testResponse, goldEntry.getGoldResult());
                zeroShotAis2CypherService.update(nullPredicted.get(i), ais, testCQP, testCypher,
                        testResponse, predictedScore, correctScore, true, provenanceMatched, resultMatch);
            }catch (Exception e){
                zeroShotAis2CypherService.update(nullPredicted.get(i), ais, testCQP, testCypher,
                        null, predictedScore, correctScore, false, false, false);
            }
            System.out.println("Evaluation entry reprocessed. id --> " + nullPredicted.get(i).getId());
        }
    }
    public void reProcessFailedAIS(List<AIS> aisList){
        String modelName = "qwen/qwen3-32b";
        List<ZeroShotAis2Cypher> nullPredicted = zeroShotAis2CypherService.getNullPredictedAIS(modelName);
        for(int i = 0; i < nullPredicted.size(); i++){
            AIS ais = null;
            if (aisList != null && !aisList.isEmpty() && i < aisList.size()) ais = aisList.get(i);
            GoldEntry goldEntry = nullPredicted.get(i).getGoldEntry();
            CQP goldCQP = LocalMapper.read(goldEntry.getGoldCqp(), CQP.class);
            CQP testCQP = cqpCompiler.mapToCQP(ais);
            Long predictedScore = ExactMatchAccuracy.getPredictedScore(testCQP);
            Long correctScore = ExactMatchAccuracy.getCorrectScore(goldCQP, testCQP);
            String testCypher = cypherBuilder.build(testCQP);
            OlapCypherResponse testResponse;
            try{
                testResponse = OlapCypherResponseMapper.map(neo4jService.fetch(testCypher), testCQP.getReturnClauses());
                boolean provenanceMatched = checkProvenance(testResponse, goldEntry.getGoldProvenance());
                boolean resultMatch = checkResult(testResponse, goldEntry.getGoldResult());
                zeroShotAis2CypherService.update(nullPredicted.get(i), ais, testCQP, testCypher,
                        testResponse, predictedScore, correctScore, true, provenanceMatched, resultMatch);
            }catch (Exception e){
                zeroShotAis2CypherService.update(nullPredicted.get(i), ais, testCQP, testCypher,
                        null, predictedScore, correctScore, false, false, false);
            }
            System.out.println("Evaluation entry reprocessed. id --> " + nullPredicted.get(i).getId());
        }
    }
}
