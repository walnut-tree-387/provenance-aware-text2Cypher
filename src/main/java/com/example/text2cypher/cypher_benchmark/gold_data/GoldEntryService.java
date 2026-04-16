package com.example.text2cypher.cypher_benchmark.gold_data;

import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.evaluation_split.EvaluationSplitService;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class GoldEntryService {
    private final GoldEntryRepository goldEntryRepository;
    private static final Long SEED_VALUE = 42L;

    public GoldEntryService(GoldEntryRepository goldEntryRepository) {
        this.goldEntryRepository = goldEntryRepository;
    }
    public void create(QueryType queryType, String modelName, String protoNL, String goldCypher, String goldProvenance,
                       String goldResult, String goldCqp, String nlQuestion) {
        GoldEntry goldEntry = new GoldEntry();
        goldEntry.setGoldCqp(goldCqp);
        goldEntry.setQueryType(queryType);
        goldEntry.setModelName(modelName);
        goldEntry.setProtoNL(protoNL);
        goldEntry.setGoldCypher(goldCypher);
        goldEntry.setGoldResult(goldResult);
        goldEntry.setGoldProvenance(goldProvenance);
        goldEntry.setQuestion(nlQuestion);
        goldEntryRepository.save(goldEntry);
    }
    public long findTotalProcessedTrue(QueryType queryType) {
        return goldEntryRepository.countByQueryTypeAndProcessedTrue(queryType);
    }
    public GoldEntry findRandomlySelectedGoldEntry(QueryType queryType) {
        long totalProcessedTrue = findTotalProcessedTrue(queryType);
        if(totalProcessedTrue >= 400) throw new RuntimeException("We already evaluated 400 entry for this query type");
        List<GoldEntry> list =
                goldEntryRepository.findRandomUnprocessedByQueryType(queryType, PageRequest.of(0,1));
        Optional<GoldEntry> entry = list.stream().findFirst();
        return entry.orElse(null);
    }
    public List<GoldEntry> findRandomlySelectedGoldEntryList(QueryType queryType) {
        long totalProcessedTrue = findTotalProcessedTrue(queryType);
        if(totalProcessedTrue >= 900) throw new RuntimeException("We already evaluated 100 entry for this query type");
        return goldEntryRepository.findRandomUnprocessedByQueryType(queryType, PageRequest.of(0,1));
    }
//    @Scheduled(fixedDelay = 20 * 1000)
//    public void seedEvaluation() {
//        for(QueryType queryType : QueryType.values()){
//            List<GoldEntry> goldEntries = goldEntryRepository.findAllByQueryType(queryType);
//            Collections.shuffle(goldEntries, new Random(SEED_VALUE));
//            splitService.createEvaluationSplit(goldEntries.subList(0, 200), 1L);
//            splitService.createEvaluationSplit(goldEntries.subList(200, 400), 2L);
//            splitService.createEvaluationSplit(goldEntries.subList(400, 600), 3L);
//        }
//    }
    public GoldEntry findById(Long id){
        Optional<GoldEntry> goldEntry = goldEntryRepository.findById(id);
        if(goldEntry.isEmpty()) throw new RuntimeException("Gold entry with id " + id + " not found");
        return goldEntry.get();
    }
}
