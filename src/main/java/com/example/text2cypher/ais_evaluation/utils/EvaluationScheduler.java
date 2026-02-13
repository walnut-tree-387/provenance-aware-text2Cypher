package com.example.text2cypher.ais_evaluation.utils;

import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.ais_evaluation.compiler.AIStoCQPCompiler;
import com.example.text2cypher.ais_evaluation.record.EvaluationService;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntry;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntryRepository;
import com.example.text2cypher.cypher_utils.cqp.CQP;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherBuilder;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponse;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponseMapper;
import com.example.text2cypher.neo4j.Neo4jService;
import com.example.text2cypher.utils.SleeperCoach;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.example.text2cypher.ais_evaluation.utils.ProvenanceChecker.checkProvenance;

@Component
public class EvaluationScheduler {
    private final GoldEntryRepository goldEntryRepository;
    private final EvaluationService evaluationService;
    private final Neo4jService neo4jService;
    private final AIStoCQPCompiler cqpCompiler;
    private final AISGenerator aisGenerator;
    private final OlapCypherBuilder cypherBuilder;

    public EvaluationScheduler(GoldEntryRepository goldEntryRepository, EvaluationService evaluationService, Neo4jService neo4jService, AIStoCQPCompiler cqpCompiler, AISGenerator aisGenerator, OlapCypherBuilder cypherBuilder) {
        this.goldEntryRepository = goldEntryRepository;
        this.evaluationService = evaluationService;
        this.neo4jService = neo4jService;
        this.cqpCompiler = cqpCompiler;
        this.aisGenerator = aisGenerator;
        this.cypherBuilder = cypherBuilder;
    }

//    @Scheduled(fixedRate = 4 * 60 * 1000)
    public void process(){
        List<GoldEntry> goldEntries = goldEntryRepository.findByProcessed();
        for (GoldEntry goldEntry : goldEntries) {
            Map<String, AIS> aisList = aisGenerator.generateAIS(goldEntry.getQuestion());
            for(String key:  aisList.keySet()){
                AIS ais = aisList.get(key);
                CQP testCQP = cqpCompiler.mapToCQP(ais);
                Long predictedScore = ExactMatchAccuracy.getPredictedScore(testCQP);
                Long correctScore = ExactMatchAccuracy.getCorrectScore(goldEntry.getGoldCqp(), testCQP);
                String testCypher = cypherBuilder.build(testCQP);
                OlapCypherResponse testResponse = null;
                try{
                    testResponse = OlapCypherResponseMapper.map(neo4jService.fetch(testCypher), testCQP.getReturnClauses());
                    boolean provenanceMatched = checkProvenance(testResponse, goldEntry.getGoldProvenance(), goldEntry.getGoldResult());
                    evaluationService.create(key, ais, goldEntry.getQuestion(), goldEntry, testCQP, testCypher, testResponse, predictedScore, correctScore, true, provenanceMatched);
                }catch (Exception e){
                    evaluationService.create(key, ais, goldEntry.getQuestion(), goldEntry, testCQP, testCypher, null, predictedScore, correctScore, false, false);
                }
                SleeperCoach.sleepMinutes(1);
//                goldEntry.setProcessed(true);
//                goldEntryRepository.save(goldEntry);
            }
        }
    }
}
