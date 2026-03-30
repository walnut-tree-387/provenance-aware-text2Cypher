package com.example.text2cypher.ais_evaluation.utils;
import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.utils.LocalMapper;
import org.springframework.stereotype.Service;

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
            return LocalMapper.readList(json, AIS.class);
        } catch (Exception e) {
            return List.of(); // never return null
        }
    }
    ///  TODO NEED TO RETURN CYPHER STRING LIST FROM THIS METHOD
    public List<String> normalizeCypher(String llmOutput) {
        String cleaned = preprocess(llmOutput);
        String json = extractJson(cleaned);
        json = repairJson(json);
        try {
            return LocalMapper.readList(json, String.class);
        } catch (Exception e) {
            System.out.println("FAILED JSON:\n" + json);
            return List.of();
        }
    }
    private String preprocess(String raw) {
        String s = raw;
        s = s.replaceAll("(?s)<think>.*?</think>", "");
        s = s.replaceAll("(?s)<analysis>.*?</analysis>", "");
        s = s.replaceAll("(?m)^```\\s*json\\s*$", "");
        s = s.replaceAll("(?m)^```\\s*$", "");
        s = s.replaceAll("(?m)^```\\s*$", "");
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
