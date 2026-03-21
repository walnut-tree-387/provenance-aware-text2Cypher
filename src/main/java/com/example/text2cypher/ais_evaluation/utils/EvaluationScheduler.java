package com.example.text2cypher.ais_evaluation.utils;

import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.ais_evaluation.compiler.AIStoCQPCompiler;
import com.example.text2cypher.ais_evaluation.record.CypherService;
import com.example.text2cypher.ais_evaluation.record.EvaluationRecord;
import com.example.text2cypher.ais_evaluation.record.EvaluationService;
import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntry;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntryRepository;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntryService;
import com.example.text2cypher.cypher_utils.cqp.CQP;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherBuilder;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponse;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponseMapper;
import com.example.text2cypher.graph_generation.csv_parser.CsvParserImpl;
import com.example.text2cypher.graph_generation.dto.FineTuneData;
import com.example.text2cypher.neo4j.Neo4jService;
import com.example.text2cypher.utils.LocalMapper;
import jakarta.transaction.Transactional;
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
    private final CsvParserImpl csvParser;
    private final CypherGenerator cypherGenerator;
    private final CypherService cypherService;

    public EvaluationScheduler(GoldEntryRepository goldEntryRepository, GoldEntryService goldEntryService, EvaluationService evaluationService, Neo4jService neo4jService, AIStoCQPCompiler cqpCompiler, AISGenerator aisGenerator, OlapCypherBuilder cypherBuilder, CsvParserImpl csvParser, CypherGenerator cypherGenerator, CypherService cypherService) {
        this.goldEntryRepository = goldEntryRepository;
        this.goldEntryService = goldEntryService;
        this.evaluationService = evaluationService;
        this.neo4jService = neo4jService;
        this.cqpCompiler = cqpCompiler;
        this.aisGenerator = aisGenerator;
        this.cypherBuilder = cypherBuilder;
        this.csvParser = csvParser;
        this.cypherGenerator = cypherGenerator;
        this.cypherService = cypherService;
    }
    @Transactional(rollbackOn =  EvaluationRollbackException.class)
//    @Scheduled(fixedDelay = 20 * 1000)
    public void process(){
        GoldEntry goldEntry = goldEntryService.findRandomlySelectedGoldEntry(QueryType.RANKING);
        if(goldEntry != null){
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
                OlapCypherResponse testResponse;
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
    @Transactional(rollbackOn =  EvaluationRollbackException.class)
//    @Scheduled(fixedDelay = 20 * 1000)
    public void processBatch(){
        List<GoldEntry> goldEntries = goldEntryService.findRandomlySelectedGoldEntryList(QueryType.COUNT);
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
    @Transactional(rollbackOn =  EvaluationRollbackException.class)
//    @Scheduled(fixedDelay = 20 * 1000)
    public void reProcessNullPredictedAIS(){
        String modelName = "openai/gpt-oss-120b";
        List<EvaluationRecord> records = evaluationService.getNullPredictedAIS(modelName);
        List<AIS> aisList = aisGenerator.generateAISBatchForNullPredictedAIS(records, modelName);
        for(int i = 0; i < records.size(); i++){
            AIS ais = null;
            if (aisList != null && !aisList.isEmpty() && i < aisList.size()) ais = aisList.get(i);
            GoldEntry goldEntry = records.get(i).getGoldEntry();
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
                evaluationService.update(records.get(i), ais, testCQP, testCypher,
                        testResponse, predictedScore, correctScore, true, provenanceMatched, resultMatch);
            }catch (Exception e){
                evaluationService.update(records.get(i), ais, testCQP, testCypher,
                        null, predictedScore, correctScore, false, false, false);
            }
            System.out.println("Evaluation entry reprocessed. id --> " + records.get(i).getId());
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
            boolean provenanceMatched;
            boolean resultMatch;
            boolean executed;
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
    public void generateFineTuneData(){
        List<FineTuneData> answers = new ArrayList<>();
        for(QueryType queryType:QueryType.values()){
            List<GoldEntry> entries = goldEntryRepository.findTop700ByQueryTypeOrderByIdAsc(queryType);
            entries.forEach(entry -> {
                answers.add(new FineTuneData(
                        entry.getGoldCqp(),
                        entry.getQueryType().toString(),
                        entry.getQuestion()
                ));
            });
        }
        csvParser.createFineTuneCsv(answers);
    }
    @Transactional(rollbackOn =  EvaluationRollbackException.class)
    public void reProcessFailedAIS(List<AIS> aisList){
        String modelName = "qwen/qwen3-32b";
        List<EvaluationRecord> records = evaluationService.getNullPredictedAIS(modelName);
        for(int i = 0; i < records.size(); i++){
            AIS ais = null;
            if (aisList != null && !aisList.isEmpty() && i < aisList.size()) ais = aisList.get(i);
            GoldEntry goldEntry = records.get(i).getGoldEntry();
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
                evaluationService.update(records.get(i), ais, testCQP, testCypher,
                        testResponse, predictedScore, correctScore, true, provenanceMatched, resultMatch);
            }catch (Exception e){
                evaluationService.update(records.get(i), ais, testCQP, testCypher,
                        null, predictedScore, correctScore, false, false, false);
            }
            System.out.println("Evaluation entry reprocessed. id --> " + records.get(i).getId());
        }
    }

    @Scheduled(fixedDelay = 20 * 1000)
    public void processCypherBatch(){
        List<GoldEntry> goldEntries = goldEntryService.findRandomlySelectedGoldEntryList(QueryType.PRIORITY_ORDER);
        Map<String, List<String>> cypherMap;
        try{
            cypherMap = cypherGenerator.generateCypherBatch(goldEntries);
        } catch(Exception e){
            throw new EvaluationRollbackException("AIS generation failed");
        }
        for(int i = 0; i < goldEntries.size(); i++){
            GoldEntry goldEntry = goldEntries.get(i);
            for(String key:  cypherMap.keySet()){
                List<String> cypherList = null;
                String testCypher = null;
                if(cypherMap.get(key) != null)cypherList = cypherMap.get(key);
                if (cypherList != null && !cypherList.isEmpty() && i < cypherList.size()) testCypher = cypherList.get(i);
                List<String> returnClauses = List.of(); // Find the return clauses from cypher string
                OlapCypherResponse testResponse;
                try{
                    testResponse = OlapCypherResponseMapper.map(neo4jService.fetch(testCypher), returnClauses);
                    boolean provenanceMatched = checkProvenance(testResponse, goldEntry.getGoldProvenance());
                    boolean resultMatch = checkResult(testResponse, goldEntry.getGoldResult());
                    cypherService.create(key, goldEntry.getQuestion(), goldEntry, testCypher,
                            testResponse, true, provenanceMatched, resultMatch);
                }catch (Exception e){
                    cypherService.create(key, goldEntry.getQuestion(), goldEntry, testCypher,
                            null, false, false, false);
                }
            }
            System.out.println("Gold entry processed. id --> " + goldEntry.getId());
            goldEntry.setProcessed(true);
            goldEntryRepository.save(goldEntry);
        }
    }
}
