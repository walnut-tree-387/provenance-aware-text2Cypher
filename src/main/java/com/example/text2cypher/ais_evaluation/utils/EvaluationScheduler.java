package com.example.text2cypher.ais_evaluation.utils;

import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.ais_evaluation.compiler.AIStoCQPCompiler;
import com.example.text2cypher.ais_evaluation.record.EvaluationService;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntry;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntryRepository;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntryService;
import com.example.text2cypher.cypher_utils.cqp.CQP;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherBuilder;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponse;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponseMapper;
import com.example.text2cypher.neo4j.Neo4jService;
import com.example.text2cypher.utils.LocalMapper;
import com.example.text2cypher.utils.SleeperCoach;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.example.text2cypher.ais_evaluation.utils.ProvenanceChecker.checkProvenance;
import static com.example.text2cypher.ais_evaluation.utils.ProvenanceChecker.checkResult;

@Component
public class EvaluationScheduler {
    private final GoldEntryRepository goldEntryRepository;
    private final GoldEntryService goldEntryService;
    private final EvaluationService evaluationService;
    private final Neo4jService neo4jService;
    private final AIStoCQPCompiler cqpCompiler;
    private final AISGenerator aisGenerator;
    private final OlapCypherBuilder cypherBuilder;

    public EvaluationScheduler(GoldEntryRepository goldEntryRepository, GoldEntryService goldEntryService, EvaluationService evaluationService, Neo4jService neo4jService, AIStoCQPCompiler cqpCompiler, AISGenerator aisGenerator, OlapCypherBuilder cypherBuilder) {
        this.goldEntryRepository = goldEntryRepository;
        this.goldEntryService = goldEntryService;
        this.evaluationService = evaluationService;
        this.neo4jService = neo4jService;
        this.cqpCompiler = cqpCompiler;
        this.aisGenerator = aisGenerator;
        this.cypherBuilder = cypherBuilder;
    }
    @Transactional(rollbackOn =  EvaluationRollbackException.class)
    @Scheduled(fixedDelay = 60 * 1000)
    public void process(){
        Optional<GoldEntry> goldEntryOp = goldEntryRepository.findFirstByProcessedFalseOrderByIdAsc();
        if(goldEntryOp.isPresent()){
            GoldEntry goldEntry = goldEntryOp.get();
            CQP goldCQP = LocalMapper.read(goldEntry.getGoldCqp(), CQP.class);
            Map<String, AIS> aisList;
            try{
                aisList = aisGenerator.generateAIS(goldEntry);
            } catch(Exception e){
                throw new EvaluationRollbackException("AIS generation failed");
            }
            if(aisList.size() != 4) throw new EvaluationRollbackException("Expected 4 Ais per gold entries but got " + aisList.size());
            for(String key:  aisList.keySet()){
                AIS ais = aisList.get(key);
                CQP testCQP = cqpCompiler.mapToCQP(ais);
                Long predictedScore = ExactMatchAccuracy.getPredictedScore(testCQP);
                Long correctScore = ExactMatchAccuracy.getCorrectScore(goldCQP, testCQP);
                String testCypher = cypherBuilder.build(testCQP);
                OlapCypherResponse testResponse = null;
                try{
                    testResponse = OlapCypherResponseMapper.map(neo4jService.fetch(testCypher), testCQP.getReturnClauses());
                    boolean provenanceMatched = checkProvenance(testResponse, goldEntry.getGoldProvenance());
                    boolean resultMatch = checkResult(testResponse, goldEntry.getGoldResult());
                    evaluationService.create(key, ais, goldEntry.getQuestion(), goldEntry, testCQP, testCypher,
                            testResponse, predictedScore, correctScore, true, provenanceMatched, resultMatch);
                }catch (Exception e){
                    evaluationService.create(key, ais, goldEntry.getQuestion(), goldEntry, testCQP, testCypher,
                            null, predictedScore, correctScore, false, false, false);
                }
            }
            System.out.println("Gold entry processed. id --> " + goldEntry.getId());
            goldEntry.setProcessed(true);
            goldEntryRepository.save(goldEntry);
        }
    }
    public Map<String, Map<AIS, List<Object>> > evaluateGoldEntry(Long id){
        GoldEntry goldEntry = goldEntryService.findById(id);
        CQP goldCQP = LocalMapper.read(goldEntry.getGoldCqp(), CQP.class);
        Map<String, AIS> aisList = aisGenerator.generateAIS(goldEntry);
        Map<String, Map<AIS, List<Object>> > result = new HashMap<>();
        for(String key:  aisList.keySet()) {
            AIS ais = aisList.get(key);
            if(ais == null)continue;
            CQP testCQP = cqpCompiler.mapToCQP(ais);
            Long predictedScore = ExactMatchAccuracy.getPredictedScore(testCQP);
            Long correctScore = ExactMatchAccuracy.getCorrectScore(goldCQP, testCQP);
            String predictedCypher = cypherBuilder.build(testCQP);
            boolean provenanceMatched = false;
            boolean resultMatch = false;
            boolean executed = false;
            OlapCypherResponse predictedResponse = null;
            try{
                predictedResponse = OlapCypherResponseMapper.map(neo4jService.fetch(predictedCypher), testCQP.getReturnClauses());
                executed = true;
                provenanceMatched = checkProvenance(predictedResponse, goldEntry.getGoldProvenance());
                resultMatch = checkResult(predictedResponse, goldEntry.getGoldResult());
            }catch (Exception e){
                continue;
            }
            result.put(key, Map.of(ais, List.of(predictedScore, correctScore, executed, provenanceMatched, resultMatch)));
        }
        return result;
    }
    public Long checkAis(AIS ais, Long goldEntry){
        GoldEntry entry = goldEntryService.findById(goldEntry);
        CQP goldCQP = LocalMapper.read(entry.getGoldCqp(), CQP.class);
        CQP testCQP = cqpCompiler.mapToCQP(ais);
        Long predictedScore = ExactMatchAccuracy.getPredictedScore(testCQP);
        return ExactMatchAccuracy.getCorrectScore(goldCQP, testCQP);
    }
}
