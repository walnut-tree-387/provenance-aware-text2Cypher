package com.example.text2cypher.ais_evaluation.utils;
import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.utils.LocalMapper;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Service
public class AISNormalizer {
    public AIS normalizeAIS(String llmOutput) {
        String cleaned = preprocess(llmOutput);
        String json = extractJson(cleaned);
        try{
            return LocalMapper.read(json, AIS.class);
        }catch(Exception e){
            return null;
        }
    }
    public List<AIS> normalizeAISList(String llmOutput, String modelName) {
        String cleaned = preprocess(llmOutput);
        String json = extractJson(cleaned);
        json = repairJson(json);
        System.out.println("Model: " + modelName + "\n Json Produced : " + json + "\n");
        try {
            JsonNode response = LocalMapper.convertToJsonNode(json);
            return LocalMapper.readListOneByOne(response, AIS.class);
        } catch (Exception e) {
            return List.of(); // never return null
        }
    }
    public List<String> normalizeCypher(String llmOutput) {
        String cleaned = preprocess(llmOutput);
        String json = extractJson(cleaned);
        List<String> cyphers;
        try{
            cyphers = LocalMapper.readList(json, String.class);
            if(cyphers.size() < 15) System.out.println(json);
            return cyphers.stream().map(this::cleanEachQueryString).toList();
        } catch(Exception e){
            cyphers = extractCypherStrings(json);
            System.out.println("Failed to normalize cypher list coming from llm output : " + json);
            return cyphers;
        }
    }
    private String cleanEachQueryString(String cypher) {
        if (cypher == null) return null;
        return cypher
                .replace("\\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
    private List<String> extractCypherStrings(String text) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                if (inString) current.append('"');
                i++;
                continue;
            }
            if (c == '"') {
                if (inString) {
                    result.add(current.toString().trim());
                    current.setLength(0);
                    inString = false;
                } else {
                    inString = true;
                }
                continue;
            }
            if (inString) {
                current.append(c);
            }
        }
        result.removeIf(s -> !s.startsWith("MATCH"));
        return result;
    }
    private String preprocess(String raw) {
        String s = raw;
        s = s.replaceAll("(?s)<think>.*?</think>", "");
        s = s.replaceAll("(?s)<analysis>.*?</analysis>", "");
        s = s.replaceAll("(?m)^```\\s*json\\s*$", "");
        s = s.replaceAll("(?m)^```\\s*$", "");
        s = s.replaceAll("(?m)^```\\s*$", "");
        s = s.replaceAll(",\\n\\s*", ",");
        return s.trim();
    }
    private String repairJson(String json) {
        json = json.replaceAll("}\\s*\\{", "},{");
        json = json.replaceAll(",\\s*([}\\]])", "$1");
        int openBraces = json.length() - json.replace("{", "").length();
        int closeBraces = json.length() - json.replace("}", "").length();
        StringBuilder jsonBuilder = new StringBuilder(json);
        while (closeBraces < openBraces) {
            jsonBuilder.append("}");
            closeBraces++;
        }
        json = jsonBuilder.toString();
        return json;
    }
    private String extractJson(String text) {
        text = text.trim();
        // Handle array
        int arrStart = text.indexOf("[");
        int arrEnd = text.lastIndexOf("]");
        if (arrStart >= 0 && arrEnd > arrStart) {
            return text.substring(arrStart, arrEnd + 1);
        }
        // Handle single object
        int objStart = text.indexOf("{");
        int objEnd = text.lastIndexOf("}");
        if (objStart >= 0 && objEnd > objStart) {
            return text.substring(objStart, objEnd + 1);
        }
        throw new IllegalStateException("No JSON found");
    }
}
