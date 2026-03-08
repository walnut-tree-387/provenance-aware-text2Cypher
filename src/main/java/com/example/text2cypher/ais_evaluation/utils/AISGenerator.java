package com.example.text2cypher.ais_evaluation.utils;
import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntry;
import com.example.text2cypher.webclients.dto.GroqChatResponse;
import com.example.text2cypher.webclients.dto.OllamaChatResponse;
import com.example.text2cypher.webclients.groq.GroqClient;
import com.example.text2cypher.cypher_benchmark.paraphraser.ParaphraseNormalizer;
import com.example.text2cypher.utils.SleeperCoach;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AISGenerator {
    private final AISPromptBuilder promptBuilder;
    private final GroqClient  groqClient;
    private final AISNormalizer answerNormalizer;
    private final Map<Long, Map<String, AIS>> aisMap = new HashMap<>();

    public AISGenerator(AISPromptBuilder promptBuilder, GroqClient groqClient, AISNormalizer answerNormalizer) {
        this.promptBuilder = promptBuilder;
        this.groqClient = groqClient;
        this.answerNormalizer = answerNormalizer;
    }

    public List<String> generateCypher(String question) {
        String prompt = promptBuilder.buildPrompt(question);
        List<String> models = List.of(
                "openai/gpt-oss-120b",
                "llama-3.3-70b-versatile",
                "moonshotai/kimi-k2-instruct-0905",
                "qwen/qwen3-32b"
        );
        List<String> answers = new ArrayList<>();
        for(String model: models){
            GroqChatResponse response = groqClient.chatCompletion(0.0f, prompt, model);
            String rawText = response.getChoices()
                    .getFirst()
                    .getMessage()
                    .getContent();
            answers.add(ParaphraseNormalizer.cypherNormalize(rawText, model));
        }
        return answers;
    }
    public Map<String, AIS> generateAIS(GoldEntry goldEntry) {
        Map<String, AIS> modelMap =
                aisMap.computeIfAbsent(goldEntry.getId(), k -> new HashMap<>());
        String prompt = promptBuilder.buildAISPrompt(goldEntry.getQuestion());
        List<String> models = List.of(
                "openai/gpt-oss-120b",
                "llama-3.3-70b-versatile",
                "moonshotai/kimi-k2-instruct-0905",
                "qwen/qwen3-32b"
        );
        long cycle = 0;
        for(String model: models){
            if (modelMap.containsKey(model))continue;
            GroqChatResponse response = groqClient.chatCompletion(0.0f, prompt, model);
            cycle++;
            if(response == null) throw new RuntimeException("Response came null for " + model + " while generating AIS");
            String rawText = response.getChoices()
                    .getFirst()
                    .getMessage()
                    .getContent();
            AIS ais = answerNormalizer.normalizeAIS(rawText);
            modelMap.put(model, ais);
            if(cycle <= 3)SleeperCoach.sleepMinutes(20000);
        }
        if(modelMap.size() == 4)aisMap.remove(goldEntry.getId());
        return modelMap;
    }
}
