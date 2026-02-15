package com.example.text2cypher.cypher_benchmark.paraphraser;

import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.groq.client.GroqClient;
import com.example.text2cypher.groq.dto.GroqChatResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProtoNLParaphraser {
    private final GroqClient groqClient;
    private final ParaphrasingPromptBuilder promptBuilder;

    public ProtoNLParaphraser(GroqClient groqClient, ParaphrasingPromptBuilder promptBuilder) {
        this.groqClient = groqClient;
        this.promptBuilder = promptBuilder;
    }

    public List<String> paraphrase(QueryType queryType, String protoNL) {
        List<String> paraphrases = new ArrayList<>();
        String prompt = promptBuilder.buildParaphrasePrompt(queryType, protoNL);
        List<String> models = List.of(
                "openai/gpt-oss-120b",
                "llama-3.3-70b-versatile",
                "qwen/qwen3-32b",
                "moonshotai/kimi-k2-instruct-0905"
        );
        for(String model: models){
            GroqChatResponse response = groqClient.chatCompletion(prompt, model);
            String rawText = response.getChoices()
                    .getFirst()
                    .getMessage()
                    .getContent();
            paraphrases.add(ParaphraseNormalizer.paraphraseNormalize(rawText, model));
        }
        return paraphrases;
    }
}
