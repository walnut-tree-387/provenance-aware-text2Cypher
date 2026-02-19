package com.example.text2cypher.ais_evaluation.utils;
import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.utils.LocalMapper;
import org.springframework.stereotype.Service;

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
    private String preprocess(String raw) {
        String s = raw;
        s = s.replaceAll("(?s)<think>.*?</think>", "");
        s = s.replaceAll("(?s)<analysis>.*?</analysis>", "");
        s = s.replaceAll("(?m)^```\\s*json\\s*$", "");
        s = s.replaceAll("(?m)^```\\s*$", "");
        s = s.replaceAll("(?m)^```\\s*$", "");
        return s.trim();
    }
    private String extractJson(String text) {
        System.out.println(text);
        if (text.startsWith("[") && text.contains("{")) {
            int objStart = text.indexOf("{");
            int objEnd = text.lastIndexOf("}");
            return text.substring(objStart, objEnd + 1);
        }
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        throw new IllegalStateException("No JSON object found");
    }
}
