package com.example.text2cypher.ais_evaluation.utils;

import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.ais_evaluation.compiler.AIStoCQPCompiler;
import com.example.text2cypher.ais_evaluation.record.CypherService;
import com.example.text2cypher.ais_evaluation.record.EvaluationRecord;
import com.example.text2cypher.ais_evaluation.record.EvaluationService;
import com.example.text2cypher.ais_evaluation.record.FewShotCypherService;
import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntry;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntryRepository;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntryService;
import com.example.text2cypher.cypher_utils.cqp.AggregationType;
import com.example.text2cypher.cypher_utils.cqp.CQP;
import com.example.text2cypher.cypher_utils.cqp.Filter;
import com.example.text2cypher.cypher_utils.cqp.Measure;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherBuilder;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponse;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponseMapper;
import com.example.text2cypher.evaluation_split.EvaluationSplitService;
import com.example.text2cypher.evaluation_split.few_shot_direct_cypher.FewShotDirectCypher;
import com.example.text2cypher.evaluation_split.few_shot_direct_cypher.FewShotDirectCypherService;
import com.example.text2cypher.evaluation_split.zero_shot_direct_cypher.ZeroShotDirectCypher;
import com.example.text2cypher.evaluation_split.zero_shot_direct_cypher.ZeroShotDirectCypherService;
import com.example.text2cypher.graph_generation.csv_parser.CsvParserImpl;
import com.example.text2cypher.graph_generation.dto.FineTuneData;
import com.example.text2cypher.neo4j.Neo4jService;
import com.example.text2cypher.utils.LocalMapper;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.example.text2cypher.ais_evaluation.utils.ProvenanceChecker.*;

@Component
public class EvaluationScheduler {
    private static final Long evaluationStage = 3L;
    private final GoldEntryRepository goldEntryRepository;
    private final GoldEntryService goldEntryService;
    private final EvaluationService evaluationService;
    private final Neo4jService neo4jService;
    private final AIStoCQPCompiler cqpCompiler;
    private final AISGenerator aisGenerator;
    private final OlapCypherBuilder cypherBuilder;
    private final CsvParserImpl csvParser;
    private final CypherGenerator cypherGenerator;
    private final FewShotDirectCypherService fewShotDirectCypherService;
    private final ZeroShotDirectCypherService zeroShotDirectCypherService;
    private final EvaluationSplitService evaluationSplitService;

    public EvaluationScheduler(GoldEntryRepository goldEntryRepository, GoldEntryService goldEntryService, EvaluationService evaluationService,
                               Neo4jService neo4jService, AIStoCQPCompiler cqpCompiler, AISGenerator aisGenerator, OlapCypherBuilder cypherBuilder,
                               CsvParserImpl csvParser, CypherGenerator cypherGenerator, FewShotDirectCypherService fewShotDirectCypherService,
                               ZeroShotDirectCypherService zeroShotDirectCypherService, EvaluationSplitService evaluationSplitService) {
        this.goldEntryRepository = goldEntryRepository;
        this.goldEntryService = goldEntryService;
        this.evaluationService = evaluationService;
        this.neo4jService = neo4jService;
        this.cqpCompiler = cqpCompiler;
        this.aisGenerator = aisGenerator;
        this.cypherBuilder = cypherBuilder;
        this.csvParser = csvParser;
        this.cypherGenerator = cypherGenerator;
        this.fewShotDirectCypherService = fewShotDirectCypherService;
        this.zeroShotDirectCypherService = zeroShotDirectCypherService;
        this.evaluationSplitService = evaluationSplitService;
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
        List<String> questions = records.stream().map(EvaluationRecord::getQuestion).toList();
        List<AIS> aisList = aisGenerator.generateAISBatchForNullPredictedAIS(questions, modelName);
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

    @Transactional(rollbackOn =  EvaluationRollbackException.class)
//    @Scheduled(fixedDelay = 20 * 1000)
    public void processCypherBatch(){
        List<GoldEntry> goldEntries = evaluationSplitService.findByQueryTypeAndEvaluationStage(QueryType.BOOLEAN, evaluationStage, 15);
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
                if(testCypher != null && !testCypher.startsWith("MATCH")) testCypher = null;
                OlapCypherResponse testResponse;
                try{
                    testResponse = OlapCypherResponseMapper.mapCypherResponse(neo4jService.fetch(testCypher));
                    boolean provenanceMatched = checkProvenanceForLLMGeneratedCypher(testResponse, goldEntry.getGoldProvenance());
                    boolean resultMatch = checkResult(testResponse, goldEntry.getGoldResult());
                    zeroShotDirectCypherService.create(key, goldEntry.getQuestion(), goldEntry, testCypher,
                            testResponse, true, provenanceMatched, resultMatch, evaluationStage);
                }catch (Exception e){
                    zeroShotDirectCypherService.create(key, goldEntry.getQuestion(), goldEntry, testCypher,
                            null, false, false, false, evaluationStage);
                }
            }
        }
        System.out.println("Evaluation finished for " + goldEntries.getFirst().getId() + " to " + goldEntries.getLast().getId());
    }
//    @Scheduled(fixedDelay = 20 * 1000)
    public void updateNullPredictedZeroShotCypher(){
        String modelName = "openai/gpt-oss-120b";
        List<ZeroShotDirectCypher> nullPredictedCyphers = zeroShotDirectCypherService.getNullPredictedCypher(modelName);
        List<GoldEntry> goldEntries = nullPredictedCyphers.stream().map(ZeroShotDirectCypher::getGoldEntry).toList();
        List<String> questions = goldEntries.stream().map(GoldEntry::getQuestion).toList();
        List<String> cypherList;
        System.out.println("Evaluation started for " + nullPredictedCyphers.getFirst().getId() + " to " + nullPredictedCyphers.getLast().getId() + " -> size : " + nullPredictedCyphers.size());
        try{
            cypherList = cypherGenerator.generateDirectCypherBatchForNullPredictedEntry(questions, modelName);
        } catch(Exception e){
            throw new EvaluationRollbackException("AIS generation failed");
        }
        for(int i = 0; i < nullPredictedCyphers.size(); i++){
            GoldEntry goldEntry = nullPredictedCyphers.get(i).getGoldEntry();
            String testCypher = null;
            if (cypherList != null && !cypherList.isEmpty() && i < cypherList.size()) testCypher = cypherList.get(i);
            if(testCypher != null && !testCypher.startsWith("MATCH")) testCypher = null;
            OlapCypherResponse testResponse;
            try{
                testResponse = OlapCypherResponseMapper.mapCypherResponse(neo4jService.fetch(testCypher));
                boolean provenanceMatched = checkProvenanceForLLMGeneratedCypher(testResponse, goldEntry.getGoldProvenance());
                boolean resultMatch = checkResult(testResponse, goldEntry.getGoldResult());
                zeroShotDirectCypherService.update(nullPredictedCyphers.get(i), testCypher, testResponse,
                        true, provenanceMatched, resultMatch);
            }catch (Exception e){
                zeroShotDirectCypherService.update(nullPredictedCyphers.get(i), testCypher, null,
                        false, false, false);
            }
        }
        System.out.println("Evaluation finished for " + nullPredictedCyphers.getFirst().getId() + " to " + nullPredictedCyphers.getLast().getId());
    }
    public void updateNullPredictedZeroShotCypherFromInput(List<String> cypherList){
        String modelName = "qwen/qwen3-32b";
        List<ZeroShotDirectCypher> nullPredictedCyphers = zeroShotDirectCypherService.getNullPredictedCypher(modelName);
        nullPredictedCyphers = nullPredictedCyphers.reversed();
        for(int i = 0; i < nullPredictedCyphers.size(); i++){
            GoldEntry goldEntry = nullPredictedCyphers.get(i).getGoldEntry();
            String testCypher = null;
            if (cypherList != null && !cypherList.isEmpty() && i < cypherList.size()) testCypher = cypherList.get(i);
            OlapCypherResponse testResponse;
            try{
                testResponse = OlapCypherResponseMapper.mapCypherResponse(neo4jService.fetch(testCypher));
                boolean provenanceMatched = checkProvenanceForLLMGeneratedCypher(testResponse, goldEntry.getGoldProvenance());
                boolean resultMatch = checkResult(testResponse, goldEntry.getGoldResult());
                zeroShotDirectCypherService.update(nullPredictedCyphers.get(i), testCypher, testResponse,
                        true, provenanceMatched, resultMatch);
            }catch (Exception e){
                zeroShotDirectCypherService.update(nullPredictedCyphers.get(i), testCypher, null,
                        false, false, false);
            }
            System.out.println("Evaluation finished for " + nullPredictedCyphers.get(i).getId());
        }
    }

//    @Scheduled(fixedDelay = 20 * 1000)
    public void processFewShotCypherBatch(){
        List<GoldEntry> goldEntries = evaluationSplitService.findByQueryTypeAndEvaluationStage(QueryType.BOOLEAN, evaluationStage, 15);
        Map<String, List<String>> cypherMap;
        try{
            cypherMap = cypherGenerator.generateCypherFewShotBatch(goldEntries);
        } catch(Exception e){
            throw new EvaluationRollbackException("Cypher generation failed");
        }
        for(int i = 0; i < goldEntries.size(); i++){
            GoldEntry goldEntry = goldEntries.get(i);
            for(String key:  cypherMap.keySet()){
                List<String> cypherList = null;
                String testCypher = null;
                if(cypherMap.get(key) != null)cypherList = cypherMap.get(key);
                if (cypherList != null && !cypherList.isEmpty() && i < cypherList.size()) testCypher = cypherList.get(i);
                if(testCypher != null && !testCypher.startsWith("MATCH")) testCypher = null;
                OlapCypherResponse testResponse;
                try{
                    testResponse = OlapCypherResponseMapper.mapCypherResponse(neo4jService.fetch(testCypher));
                    boolean provenanceMatched = checkProvenanceForLLMGeneratedCypher(testResponse, goldEntry.getGoldProvenance());
                    boolean resultMatch = checkResult(testResponse, goldEntry.getGoldResult());
                    fewShotDirectCypherService.create(key, goldEntry.getQuestion(), goldEntry, testCypher,
                            testResponse, true, provenanceMatched, resultMatch, evaluationStage);
                }catch (Exception e){
                    fewShotDirectCypherService.create(key, goldEntry.getQuestion(), goldEntry, testCypher,
                            null, false, false, false, evaluationStage);
                }
            }
        }
        System.out.println("Evaluation finished for " + goldEntries.getFirst().getId() + " to " + goldEntries.getLast().getId());
    }
//    @Scheduled(fixedDelay = 20 * 1000)
    public void updateNullPredictedFewShotCypher(){
        String modelName = "qwen/qwen3-32b";
        List<FewShotDirectCypher> nullPredictedCyphers = fewShotDirectCypherService.getNullPredictedCypher(modelName);
        List<GoldEntry> goldEntries = nullPredictedCyphers.stream().map(FewShotDirectCypher::getGoldEntry).toList();
        List<String> questions = goldEntries.stream().map(GoldEntry::getQuestion).toList();
        List<String> cypherList;
        System.out.println("Evaluation started for " + nullPredictedCyphers.getFirst().getId() + " to " + nullPredictedCyphers.getLast().getId() + " -> size : " + nullPredictedCyphers.size());
        try{
            cypherList = cypherGenerator.generateFewShotCypherBatchForNullPredictedEntry(questions, modelName);
        } catch(Exception e){
            throw new EvaluationRollbackException("AIS generation failed");
        }
        for(int i = 0; i < nullPredictedCyphers.size(); i++){
            GoldEntry goldEntry = nullPredictedCyphers.get(i).getGoldEntry();
            String testCypher = null;
            if (cypherList != null && !cypherList.isEmpty() && i < cypherList.size()) testCypher = cypherList.get(i);
            if(testCypher != null && !testCypher.startsWith("MATCH")) testCypher = null;
            OlapCypherResponse testResponse;
            try{
                testResponse = OlapCypherResponseMapper.mapCypherResponse(neo4jService.fetch(testCypher));
                boolean provenanceMatched = checkProvenanceForLLMGeneratedCypher(testResponse, goldEntry.getGoldProvenance());
                boolean resultMatch = checkResult(testResponse, goldEntry.getGoldResult());
                fewShotDirectCypherService.update(nullPredictedCyphers.get(i), testCypher, testResponse,
                        true, provenanceMatched, resultMatch);
            }catch (Exception e){
                fewShotDirectCypherService.update(nullPredictedCyphers.get(i), testCypher, null,
                        false, false, false);
            }
        }
        System.out.println("Evaluation finished for " + nullPredictedCyphers.getFirst().getId() + " to " + nullPredictedCyphers.getLast().getId());
    }


    public List<Map<String, Object>> convertFilters(List<Filter> filters){
        List<Map<String, Object>> condos = new ArrayList<>();
        for(Filter filter: filters){
            String dim = filter.getDimension().toString().toLowerCase();
            String op = filter.getOperator().getValue();
            String value = filter.getValue().toString();
            Map<String, Object> map = new HashMap<>();
            map.put("dim", dim);map.put("op", op);map.put("value", value);
            condos.add(map);
        }
        return condos;
    }
    public void generateFineTuneData(){
        List<FineTuneData> answers = new ArrayList<>();
        for(QueryType queryType:QueryType.values()){
            Sort sort = Sort.by(Sort.Direction.ASC, "id");
            Pageable pageable = PageRequest.of(0, 100, sort);
            List<GoldEntry> entries = goldEntryRepository.findRandom200GoldEntry(queryType, pageable);
            for(GoldEntry entry:entries){
                if(entry.getQueryType().equals(QueryType.COUNT) || entry.getQueryType().equals(QueryType.AGGREGATION)){
                    CQP cqp = LocalMapper.read(entry.getGoldCqp(), CQP.class);
                    List<Map<String, Object>> condos;
                    List<Map<String, Object>> measures;
                    condos = convertFilters(cqp.getFilters());
                    measures = new ArrayList<>();
                    for(Measure measure: cqp.getMeasures()){
                        String type = measure.getAggregationType().toString().toLowerCase();
                        String alias = measure.getAlias();
                        List<Map<String, Object>> filters = convertFilters(measure.getFilters());
                        Map<String, Object> map = new HashMap<>();
                        map.put("type", type);map.put("alias", alias);map.put("filters", filters);
                        measures.add(map);
                    }
                    List<String> proj = new ArrayList<>(cqp.getReturnClauses());
                    Map<String, Object> total = new HashMap<>();
                    total.put("filters", condos);
                    total.put("measures", measures);
                    total.put("projections", proj);
                    String ans = LocalMapper.write(total);
                    answers.add(new FineTuneData(
                            ans,
                            entry.getQueryType().toString(),
                            entry.getQuestion()
                    ));
                }
            }
        }
        csvParser.createFineTuneCsv(answers);
    }
    private String cleanEachQueryString(String cypher) {
        if (cypher == null) return null;
        return cypher
                .replace("\\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
//    @Scheduled(fixedDelay = 20 * 1000)
    public void updateFewShotRecordsWithFaultyCypher(){
        String modelName = "moonshotai/kimi-k2-instruct-0905";
        List<FewShotDirectCypher> records = fewShotDirectCypherService.findAll(modelName);
        int executedChg = 0, resultCng = 0, provenanceCng = 0, idx = 0;
        for(FewShotDirectCypher record:records){
            idx++;
            if(idx % 400 == 0) System.out.print("idx evaluated : " + idx);
            boolean flag = false;
            GoldEntry goldEntry = record.getGoldEntry();
            String testCypher = cleanEachQueryString(record.getPredictedCypher());
            OlapCypherResponse testResponse = null;
            boolean provenanceMatch = false;
            boolean resultMatch = false;
            boolean executed = false;
            try{
                testResponse = OlapCypherResponseMapper.mapCypherResponse(neo4jService.fetch(testCypher));
                provenanceMatch = checkProvenanceForLLMGeneratedCypher(testResponse, goldEntry.getGoldProvenance());
                resultMatch = checkResult(testResponse, goldEntry.getGoldResult());
                executed = true;
            }catch (Exception e){}
            if(!record.getProvenanceMatched() && provenanceMatch) {
                System.out.println("Mismatch in provenance for id : " + record.getId() + "\n");
                provenanceCng++;
                flag = true;
            }
            if(!record.getResultMatch() && resultMatch){
                System.out.println("Mismatch in result for id : " + record.getId() + "\n");
                resultCng++;
                flag = true;
            }
            if(!record.getExecuted() && executed) {
                System.out.println("Mismatch in executed for id : " + record.getId() + "\n");
                executedChg++;
                flag = true;
            }
            if(flag)fewShotDirectCypherService.update(record, testCypher, testResponse, executed, provenanceMatch, resultMatch);
        }
        System.out.println("Finished updating......" + executedChg + " " + resultCng + " " + provenanceCng);
    }
//    @Scheduled(fixedDelay = 20 * 1000)
    public void updateZeroShotRecordsWithFaultyCypher(){
        String modelName = "qwen/qwen3-32b";
        List<ZeroShotDirectCypher> records = zeroShotDirectCypherService.findAll(modelName);
        int executedChg = 0, resultCng = 0, provenanceCng = 0, idx = 0;
        for(ZeroShotDirectCypher record:records){
            idx++;
            if(idx % 400 == 0) System.out.print("idx evaluated : " + idx);
            boolean flag = false;
            GoldEntry goldEntry = record.getGoldEntry();
            String testCypher = cleanEachQueryString(record.getPredictedCypher());
            OlapCypherResponse testResponse = null;
            boolean provenanceMatch = false;
            boolean resultMatch = false;
            boolean executed = false;
            try{
                testResponse = OlapCypherResponseMapper.mapCypherResponse(neo4jService.fetch(testCypher));
                provenanceMatch = checkProvenanceForLLMGeneratedCypher(testResponse, goldEntry.getGoldProvenance());
                resultMatch = checkResult(testResponse, goldEntry.getGoldResult());
                executed = true;
            }catch (Exception e){}
            if(!record.getProvenanceMatched() && provenanceMatch) {
                System.out.println("Mismatch in provenance for id : " + record.getId() + "\n");
                provenanceCng++;
                flag = true;
            }
            if(!record.getResultMatch() && resultMatch){
                System.out.println("Mismatch in result for id : " + record.getId() + "\n");
                resultCng++;
                flag = true;
            }
            if(!record.getExecuted() && executed) {
                System.out.println("Mismatch in executed for id : " + record.getId() + "\n");
                executedChg++;
                flag = true;
            }
            if(flag)zeroShotDirectCypherService.update(record, testCypher, testResponse, executed, provenanceMatch, resultMatch);
        }
        System.out.println("Finished updating......" + executedChg + " " + resultCng + " " + provenanceCng);
    }
}
