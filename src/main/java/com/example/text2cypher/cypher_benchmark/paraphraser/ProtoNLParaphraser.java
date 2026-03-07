package com.example.text2cypher.cypher_benchmark.paraphraser;

import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import com.example.text2cypher.webclients.dto.OllamaChatResponse;
import com.example.text2cypher.webclients.local.LocalClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProtoNLParaphraser {
    private final LocalClient localClient;
    private final ParaphrasingPromptBuilder promptBuilder;

    public ProtoNLParaphraser(LocalClient localClient, ParaphrasingPromptBuilder promptBuilder) {
        this.localClient = localClient;
        this.promptBuilder = promptBuilder;
    }

    public Map<String, String> paraphrase(QueryType queryType, String protoNL) {
        Map<String, String> paraphrases = new HashMap<>();
        String prompt = promptBuilder.buildParaphrasePrompt(queryType, protoNL);
        List<String> models = List.of(
                "deepseek-v3.1:671b-cloud",
                "gpt-oss:120b-cloud",
                "kimi-k2:1t-cloud",
                "mistral-large-3:675b-cloud"
        );
        for (String model : models) {
            OllamaChatResponse response = localClient.chatCompletion(0.3f, prompt, model);
            String rawText = response
                    .getMessage()
                    .getContent();
            paraphrases.put(model, ParaphraseNormalizer.paraphraseNormalize(rawText, model));
        }
        return paraphrases;
    }
}
