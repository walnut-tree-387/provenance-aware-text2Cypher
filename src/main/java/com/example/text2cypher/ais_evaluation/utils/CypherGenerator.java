package com.example.text2cypher.ais_evaluation.utils;

import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntry;
import com.example.text2cypher.cypher_benchmark.paraphraser.ParaphraseNormalizer;
import com.example.text2cypher.utils.SleeperCoach;
import com.example.text2cypher.webclients.dto.GroqChatResponse;
import com.example.text2cypher.webclients.dto.OllamaChatResponse;
import com.example.text2cypher.webclients.groq.GroqClient;
import com.example.text2cypher.webclients.huggingface.HFClient;
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
    private final GroqClient groqClient;
    private final HFClient hfClient;

    public CypherGenerator(LocalClient localClient, AISNormalizer answerNormalizer, AISPromptBuilder promptBuilder, GroqClient groqClient, HFClient hfClient) {
        this.localClient = localClient;
        this.answerNormalizer = answerNormalizer;
        this.promptBuilder = promptBuilder;
        this.groqClient = groqClient;
        this.hfClient = hfClient;
    }
    public Map<String, List<String>> generateCypherBatch(List<GoldEntry> goldEntries) {
        List<String> questions = goldEntries.stream().map(GoldEntry::getQuestion).toList();
        String prompt = promptBuilder.buildPrompt(questions);
        List<String> ollamaModels = List.of("gpt-oss:120b-cloud",  "kimi-k2:1t-cloud");
        List<String> hfModels = List.of("meta-llama/Llama-3.3-70B-Instruct:groq", "Qwen/Qwen3-32B:groq");
        Map<String, List<String>> modelMap = new HashMap<>();
        long cycle = 0;
        for(String model: ollamaModels){
            OllamaChatResponse response = localClient.chatCompletion(0.0f , prompt, model);
            cycle++;
            if(response == null) throw new RuntimeException("Response came null for " + model + " while generating Cypher");
            String rawText = response
                    .getMessage()
                    .getContent();
            List<String> cyphers = answerNormalizer.normalizeCypher(rawText);
            System.out.println("Successful JSON fetched by model : " + model + "\n" + cyphers.size());
            modelMap.put(model, cyphers);
            if(cycle <= 1) SleeperCoach.sleepMinutes(20000);
        }
        cycle = 0;
        for(String model: hfModels){
            GroqChatResponse response = hfClient.chatCompletion(0.0f, prompt, model);
            cycle++;
            if(response == null) throw new RuntimeException("Response came null for " + model + " while generating Cypher");
            String rawText = response.getChoices()
                    .getFirst()
                    .getMessage()
                    .getContent();
            List<String> cyphers = answerNormalizer.normalizeCypher(rawText);
            System.out.println("Successful JSON fetched by model : " + model + "\n" + cyphers.size());
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
        String groqQwen = "qwen/qwen3-32b";
        String hfLlama = "meta-llama/Llama-3.3-70B-Instruct:groq";
        GroqChatResponse response = hfClient.chatCompletion(0.0f, prompt, hfLlama);
        if(response == null) throw new RuntimeException("Response came null for " + hfLlama + " while generating AIS");
        String rawText = response.getChoices()
                .getFirst()
                .getMessage()
                .getContent();
        List<String> cyphers = answerNormalizer.normalizeCypher(rawText);
        modelMap.put(hfLlama, cyphers);
        response = groqClient.chatCompletion(0.0f, prompt, groqQwen);
        if(response == null) throw new RuntimeException("Response came null for " + groqQwen + " while generating AIS");
        rawText = response.getChoices()
                .getFirst()
                .getMessage()
                .getContent();
        cyphers = answerNormalizer.normalizeCypher(rawText);
        modelMap.put(groqQwen, cyphers);
        return modelMap;
    }
    public List<String> generateDirectCypherBatchForNullPredictedEntry(List<String> questions, String modelName) {
        String prompt = promptBuilder.buildPrompt(questions);
        List<String> output = new ArrayList<>();
        if(modelName.equals("openai/gpt-oss-120b") || modelName.equals("moonshotai/kimi-k2-instruct-0905")){
            String tempModel = "";
            if(modelName.equals("openai/gpt-oss-120b")) tempModel = "gpt-oss:120b-cloud";
            else tempModel = "kimi-k2:1t-cloud";
            OllamaChatResponse response = localClient.chatCompletion(0.0f, prompt, tempModel);
            if(response == null) throw new RuntimeException("Response came null for " + tempModel + " while generating AIS");
            String rawText = response
                    .getMessage()
                    .getContent();
            output = answerNormalizer.normalizeCypher(rawText);
        }
        else if(modelName.equals("qwen/qwen3-32b") || modelName.equals("llama-3.3-70b-versatile")){
            String tempModel = "";
            if(modelName.equals("qwen/qwen3-32b"))tempModel = "Qwen/Qwen3-32B:groq";
            else tempModel = "meta-llama/Llama-3.3-70B-Instruct:groq";
            GroqChatResponse response = hfClient.chatCompletion(0.0f, prompt, tempModel);
            if(response == null) throw new RuntimeException("Response came null for " + modelName + " while generating AIS");
            String rawText = response.getChoices()
                    .getFirst()
                    .getMessage()
                    .getContent();
            output = answerNormalizer.normalizeCypher(rawText);
        }
        return output;
    }
}
