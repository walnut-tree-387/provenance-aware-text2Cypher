package com.example.text2cypher.cypher_benchmark.gold_data;

import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GoldEntryService {
    private final GoldEntryRepository goldEntryRepository;

    public GoldEntryService(GoldEntryRepository goldEntryRepository) {
        this.goldEntryRepository = goldEntryRepository;
    }
    public GoldEntry findById(Long id) {
        return goldEntryRepository.findById(id).orElse(null);
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
        if(totalProcessedTrue >= 400) throw new RuntimeException("We already evaluated 400 entry for this query type");
        return goldEntryRepository.findRandomUnprocessedByQueryType(queryType, PageRequest.of(0,5));
    }
}
