package com.example.text2cypher.ais_evaluation.utils;

import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntry;
import com.example.text2cypher.cypher_benchmark.paraphraser.ParaphraseNormalizer;
import com.example.text2cypher.utils.SleeperCoach;
import com.example.text2cypher.webclients.dto.GroqChatResponse;
import com.example.text2cypher.webclients.dto.OllamaChatResponse;
import com.example.text2cypher.webclients.local.LocalClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CypherGenerator {
    private final LocalClient localClient;
    private final AISNormalizer answerNormalizer;
    private final AISPromptBuilder promptBuilder;

    public CypherGenerator(LocalClient localClient, AISNormalizer answerNormalizer, AISPromptBuilder promptBuilder) {
        this.localClient = localClient;
        this.answerNormalizer = answerNormalizer;
        this.promptBuilder = promptBuilder;
    }
    public Map<String, List<String>> generateCypherBatch(List<GoldEntry> goldEntries) {
        List<String> questions = goldEntries.stream().map(GoldEntry::getQuestion).toList();
        String prompt = promptBuilder.buildPrompt(questions);
        List<String> ollamaModels = List.of("gpt-oss:120b-cloud",  "kimi-k2:1t-cloud");
        Map<String, List<String>> modelMap = new HashMap<>();
        long cycle = 0;
        for(String model: ollamaModels){
            OllamaChatResponse response = localClient.chatCompletion(0.0f , prompt, model);
            cycle++;
            if(response == null) throw new RuntimeException("Response came null for " + model + " while generating AIS");
            String rawText = response
                    .getMessage()
                    .getContent();
            List<String> cyphers = answerNormalizer.normalizeCypher(rawText);
            modelMap.put(model, cyphers);
            if(cycle <= 1) SleeperCoach.sleepMinutes(20000);
        }
        return modelMap;
    }
    public Map<String, List<String>> generateCypherFewShotBatch(List<GoldEntry> goldEntries) {
        List<String> questions = goldEntries.stream().map(GoldEntry::getQuestion).toList();
        String prompt = promptBuilder.buildFewShotPrompt(questions);
        List<String> ollamaModels = List.of("gpt-oss:120b-cloud",  "kimi-k2:1t-cloud");
        Map<String, List<String>> modelMap = new HashMap<>();
        long cycle = 0;
        for(String model: ollamaModels){
            OllamaChatResponse response = localClient.chatCompletion(0.0f , prompt, model);
            cycle++;
            if(response == null) throw new RuntimeException("Response came null for " + model + " while generating AIS");
            String rawText = response
                    .getMessage()
                    .getContent();
            List<String> cyphers = answerNormalizer.normalizeCypher(rawText);
            modelMap.put(model, cyphers);
            if(cycle <= 1) SleeperCoach.sleepMinutes(20000);
        }
        return modelMap;
    }
}
