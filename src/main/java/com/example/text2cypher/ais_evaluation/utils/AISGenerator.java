package com.example.text2cypher.ais_evaluation.utils;
import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.ais_evaluation.record.EvaluationRecord;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntry;
import com.example.text2cypher.webclients.dto.GroqChatResponse;
import com.example.text2cypher.webclients.dto.OllamaChatResponse;
import com.example.text2cypher.webclients.groq.GroqClient;
import com.example.text2cypher.cypher_benchmark.paraphraser.ParaphraseNormalizer;
import com.example.text2cypher.utils.SleeperCoach;
import com.example.text2cypher.webclients.huggingface.HFClient;
import com.example.text2cypher.webclients.local.LocalClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AISGenerator {
    private final AISPromptBuilder promptBuilder;
    private final GroqClient groqClient;
    private final HFClient hfClient;
    private final LocalClient localClient;
    private final AISNormalizer answerNormalizer;
    private final Map<Long, Map<String, AIS>> aisMap = new HashMap<>();

    public AISGenerator(AISPromptBuilder promptBuilder, GroqClient groqClient, HFClient hfClient, LocalClient localClient, AISNormalizer answerNormalizer) {
        this.promptBuilder = promptBuilder;
        this.groqClient = groqClient;
        this.hfClient = hfClient;
        this.localClient = localClient;
        this.answerNormalizer = answerNormalizer;
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
    public Map<String, List<AIS> >  generateAISBatch(List<GoldEntry> goldEntries) {
        List<String> questions = goldEntries.stream().map(GoldEntry::getQuestion).toList();
        String prompt = promptBuilder.buildAISPromptBatch(questions);
        List<String> ollamaModels = List.of("gpt-oss:120b-cloud",  "kimi-k2:1t-cloud");
        List<String> groqModels = List.of("meta-llama/Llama-3.3-70B-Instruct:groq", "Qwen/Qwen3-32B:groq");
        Map<String, List<AIS>> modelMap = new HashMap<>();
        long cycle = 0;
        for(String model: ollamaModels){
            OllamaChatResponse response = localClient.chatCompletion(0.0f , prompt, model);
            cycle++;
            if(response == null) throw new RuntimeException("Response came null for " + model + " while generating AIS");
            String rawText = response
                    .getMessage()
                    .getContent();
            List<AIS> aisList = answerNormalizer.normalizeAISList(rawText, model);
            modelMap.put(model, aisList);
            System.out.println("Response came for =======================================" + model);
            if(cycle <= 1)SleeperCoach.sleepMinutes(20000);
        }
        cycle = 0;
        for(String model: groqModels){
            GroqChatResponse response = hfClient.chatCompletion(0.0f, prompt, model);
            cycle++;
             if(response == null) throw new RuntimeException("Response came null for " + model + " while generating AIS");
            String rawText = response.getChoices()
                    .getFirst()
                    .getMessage()
                    .getContent();
            List<AIS> aisList = answerNormalizer.normalizeAISList(rawText, model);
            modelMap.put(model, aisList);
            System.out.println("Response came for =======================================" + model);
            if(cycle <= 1)SleeperCoach.sleepMinutes(20000);
        }
        return modelMap;
    }
    public List<AIS> generateAISBatchForNullPredictedAIS(List<String> questions, String modelName) {
        List<AIS> aisList = new ArrayList<>();
        String prompt = promptBuilder.buildAISPromptBatch(questions);
        if(modelName.equals("openai/gpt-oss-120b") || modelName.equals("moonshotai/kimi-k2-instruct-0905")){
            String tempModel = "";
            if(modelName.equals("openai/gpt-oss-120b")) tempModel = "gpt-oss:120b-cloud";
            else tempModel = "kimi-k2:1t-cloud";
            OllamaChatResponse response = localClient.chatCompletion(0.0f, prompt, tempModel);
            if(response == null) throw new RuntimeException("Response came null for " + tempModel + " while generating AIS");
            String rawText = response
                    .getMessage()
                    .getContent();
            aisList = answerNormalizer.normalizeAISList(rawText, modelName);
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
            aisList = answerNormalizer.normalizeAISList(rawText, modelName);
        }
        return aisList;
    }
}
