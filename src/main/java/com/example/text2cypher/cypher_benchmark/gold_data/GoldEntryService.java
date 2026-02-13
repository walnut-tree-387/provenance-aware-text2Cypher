package com.example.text2cypher.cypher_benchmark.gold_data;

import org.springframework.stereotype.Service;

@Service
public class GoldEntryService {
    private final GoldEntryRepository goldEntryRepository;

    public GoldEntryService(GoldEntryRepository goldEntryRepository) {
        this.goldEntryRepository = goldEntryRepository;
    }
    public GoldEntry findById(Long id) {
        return goldEntryRepository.findById(id).orElse(null);
    }
    public void create(String goldCypher, String goldProvenance, String goldResult, String goldCqp, String nlQuestion) {
        GoldEntry goldEntry = new GoldEntry();
        goldEntry.setGoldCqp(goldCqp);
        goldEntry.setGoldCypher(goldCypher);
        goldEntry.setGoldResult(goldResult);
        goldEntry.setGoldProvenance(goldProvenance);
        goldEntry.setQuestion(nlQuestion);
        goldEntryRepository.save(goldEntry);
    }
}
