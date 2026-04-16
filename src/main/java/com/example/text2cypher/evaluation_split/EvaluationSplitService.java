package com.example.text2cypher.evaluation_split;

import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntry;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EvaluationSplitService {
    private static int PAGE_NUMBER = 0;
    private final EvaluationSplitRepository evaluationSplitRepository;
    private final GoldEntryService goldEntryService;
    public EvaluationSplitService(EvaluationSplitRepository evaluationSplitRepository, GoldEntryService goldEntryService) {
        this.evaluationSplitRepository = evaluationSplitRepository;
        this.goldEntryService = goldEntryService;
    }
    public void createEvaluationSplit(List<GoldEntry> goldEntries, Long stage) {
        for (GoldEntry goldEntry : goldEntries) {
            EvaluationSplit evaluationSplit = new EvaluationSplit();
            evaluationSplit.setQueryType(goldEntry.getQueryType());
            evaluationSplit.setEvaluation_stage(stage);
            evaluationSplit.setGold_entry_id(goldEntry.getId());
            evaluationSplitRepository.save(evaluationSplit);
        }
    }
    public List<GoldEntry> findByQueryTypeAndEvaluationStage(QueryType queryType, Long evaluationStage, int batchSize) {
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
         List<EvaluationSplit> evaluationSplits =
                evaluationSplitRepository.findByQueryTypeAndEvaluationStage(queryType, evaluationStage, PageRequest.of(PAGE_NUMBER, batchSize, sort));
         System.out.println("Evaluation started from " + evaluationSplits.getFirst() + " to " + evaluationSplits.getLast());
         List<GoldEntry> goldEntries = new ArrayList<>();
        for (EvaluationSplit evaluationSplit : evaluationSplits) {
            goldEntries.add(goldEntryService.findById(evaluationSplit.getGold_entry_id()));
        }
        PAGE_NUMBER++;
        return goldEntries;
    }
}
