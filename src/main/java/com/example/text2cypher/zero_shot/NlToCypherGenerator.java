package com.example.text2cypher.zero_shot;

import com.example.text2cypher.paraphrasing.client.GroqClient;
import com.example.text2cypher.paraphrasing.dto.GroqChatResponse;
import com.example.text2cypher.paraphrasing.service.LLMAnswerNormalizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class NlToCypherGenerator {
    private final NlToCypherPromptBuilder promptBuilder;
    private final GroqClient  groqClient;

    public NlToCypherGenerator(NlToCypherPromptBuilder promptBuilder, GroqClient groqClient) {
        this.promptBuilder = promptBuilder;
        this.groqClient = groqClient;
    }

    public List<String> generateCypher(String question) {
        String prompt = promptBuilder.buildPrompt(question);
        List<String> models = List.of(
                "openai/gpt-oss-120b",
                "llama-3.3-70b-versatile",
                "qwen/qwen3-32b"
        );
        List<String> answers = new ArrayList<>();
        for(String model: models){
            GroqChatResponse response = groqClient.chatCompletion(prompt, model);
            String rawText = response.getChoices()
                    .getFirst()
                    .getMessage()
                    .getContent();
            answers.add(LLMAnswerNormalizer.cypherNormalize(rawText, model));
        }
        return answers;
    }
}
