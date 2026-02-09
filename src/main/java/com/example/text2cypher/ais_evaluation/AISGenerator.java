package com.example.text2cypher.ais_evaluation;
import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.groq.client.GroqClient;
import com.example.text2cypher.groq.dto.GroqChatResponse;
import com.example.text2cypher.cypher_benchmark.paraphraser.ParaphraseNormalizer;
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
            GroqChatResponse response = groqClient.chatCompletion(prompt, model);
            String rawText = response.getChoices()
                    .getFirst()
                    .getMessage()
                    .getContent();
            answers.add(ParaphraseNormalizer.cypherNormalize(rawText, model));
        }
        return answers;
    }
    public Map<String, AIS> generateAIS(String question) {
        String prompt = promptBuilder.buildAISPrompt(question);
        System.out.println("prompt Length = " + prompt.length());
        List<String> models = List.of(
                "openai/gpt-oss-120b",
                "llama-3.3-70b-versatile",
                "moonshotai/kimi-k2-instruct-0905",
                "qwen/qwen3-32b"
        );
        Map<String, AIS> answers = new HashMap<>();
        for(String model: models){
            GroqChatResponse response = groqClient.chatCompletion(prompt, model);
            if(response.getChoices() == null)continue;
            String rawText = response.getChoices()
                    .getFirst()
                    .getMessage()
                    .getContent();
            answers.put(model, answerNormalizer.normalizeAIS(rawText));
        }
        return answers;
    }
}
