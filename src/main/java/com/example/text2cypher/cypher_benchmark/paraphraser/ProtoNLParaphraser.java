package com.example.text2cypher.cypher_benchmark.paraphraser;

import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.groq.client.GroqClient;
import com.example.text2cypher.groq.dto.GroqChatResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProtoNLParaphraser {
    private final GroqClient groqClient;
    private final ParaphrasingPromptBuilder promptBuilder;

    public ProtoNLParaphraser(GroqClient groqClient, ParaphrasingPromptBuilder promptBuilder) {
        this.groqClient = groqClient;
        this.promptBuilder = promptBuilder;
    }

    public Map<String, String> paraphrase(QueryType queryType, String protoNL) {
        Map<String, String> paraphrases = new HashMap<>();
        String prompt = promptBuilder.buildParaphrasePrompt(queryType, protoNL);
        List<String> models = List.of(
                "openai/gpt-oss-120b",
                "llama-3.3-70b-versatile",
                "openai/gpt-oss-20b",
                "moonshotai/kimi-k2-instruct-0905"
        );
        for(String model: models){
            GroqChatResponse response = groqClient.chatCompletion(0.3f, prompt, model);
            String rawText = response.getChoices()
                    .getFirst()
                    .getMessage()
                    .getContent();
            paraphrases.put(model, ParaphraseNormalizer.paraphraseNormalize(rawText, model));
        }
        return paraphrases;
    }
}
