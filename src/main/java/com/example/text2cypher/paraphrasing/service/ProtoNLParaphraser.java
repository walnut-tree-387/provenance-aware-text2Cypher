package com.example.text2cypher.paraphrasing.service;

import com.example.text2cypher.paraphrasing.client.GroqClient;
import com.example.text2cypher.paraphrasing.dto.GroqChatResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ProtoNLParaphraser {
    private final GroqClient groqClient;
    private final PromptBuilder promptBuilder;

    public ProtoNLParaphraser(GroqClient groqClient, PromptBuilder promptBuilder) {
        this.groqClient = groqClient;
        this.promptBuilder = promptBuilder;
    }

    public List<String> paraphrase(String protoNL) {
        List<String> paraphrases = new ArrayList<>();
        String prompt = promptBuilder.buildPrompt(protoNL);
        List<String> models = List.of(
                "openai/gpt-oss-120b",
                "llama-3.3-70b-versatile",
                "qwen/qwen3-32b"
        );
        for(String model: models){
            GroqChatResponse response = groqClient.chatCompletion(prompt, model);
            String rawText = response.getChoices()
                    .getFirst()
                    .getMessage()
                    .getContent();
            paraphrases.add(ParaphraseNormalizer.normalize(rawText, model));
        }
        return paraphrases;
    }
}
