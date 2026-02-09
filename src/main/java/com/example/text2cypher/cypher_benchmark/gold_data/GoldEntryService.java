package com.example.text2cypher.cypher_benchmark.gold_data;

import com.example.text2cypher.ais_evaluation.AISGenerator;
import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.ais_evaluation.ais.compilers.AIStoCQPCompiler;
import com.example.text2cypher.cypher_utils.cqp.CQP;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherBuilder;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponse;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponseMapper;
import com.example.text2cypher.neo4j.Neo4jService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GoldEntryService {
    private final GoldEntryRepository goldEntryRepository;
    private final AISGenerator aisGenerator;
    private final AIStoCQPCompiler cqpCompiler;
    private final OlapCypherBuilder cypherBuilder;
    private final Neo4jService neo4jService;

    public GoldEntryService(GoldEntryRepository goldEntryRepository, AISGenerator aisGenerator, AIStoCQPCompiler cqpCompiler,
                            OlapCypherBuilder cypherBuilder, Neo4jService neo4jService) {
        this.goldEntryRepository = goldEntryRepository;
        this.aisGenerator = aisGenerator;
        this.cqpCompiler = cqpCompiler;
        this.cypherBuilder = cypherBuilder;
        this.neo4jService = neo4jService;
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

    @Scheduled(fixedRate = 3 * 60 * 1000)
    public void process(){
         List<GoldEntry> goldEntries = goldEntryRepository.findByProcessed();
        for (GoldEntry goldEntry : goldEntries) {
            Map<String, AIS> aisList = aisGenerator.generateAIS(goldEntry.getQuestion());
            for(String key:  aisList.keySet()){
                AIS ais = aisList.get(key);
                CQP testCQP = cqpCompiler.mapToCQP(ais);
                String testCypher = cypherBuilder.build(testCQP);
                OlapCypherResponse testResponse = OlapCypherResponseMapper.map(neo4jService.fetch(testCypher), testCQP.getReturnClauses());
            }
        }
    }
}
