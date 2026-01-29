package com.example.text2cypher.paraphrasing.service;
import com.example.text2cypher.data.cqp.entities.AIS.context.AISContext;
import com.example.text2cypher.data.cqp.entities.AIS.fact.AISFact;
import com.example.text2cypher.data.cqp.entities.AIS.fact.AISFactNode;
import com.example.text2cypher.data.cqp.entities.AIS.intent.AISIntent;
import com.example.text2cypher.data.cqp.entities.AIS.projection.AISProjection;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AISNormalizer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    public Map<String, Object> normalizeAIS(String llmOutput) {
        String cleaned = preprocess(llmOutput);
        String json = extractJson(cleaned);

        Map<String, Object> raw;
        try {
            raw = objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse JSON from LLM output", e);
        }
        return raw;

//        AIS ais = new AIS();
//
//        // Fact
//        ais.setFact(normalizeFact(firstNonNull(raw.get("fact"), raw.get("AISFact"))));
//
//        // Context
//        ais.setContext(normalizeContext(firstNonNull(raw.get("context"), raw.get("AISContext"))));
//
//        // Axes (empty for now, can extend later)
//        ais.setAxes(List.of());
//
//        // Intents
//        ais.setIntents(normalizeIntents(firstNonNull(raw.get("intents"), raw.get("AISIntent"))));
//
//        // Derived Intents
//        ais.setDerivedIntents(List.of());
//
//        // Order Intent
//        ais.setOrderIntents(null);
//
//        // Limit
//        Object limitObj = raw.get("limit");
//        if (limitObj instanceof Integer i) {
//            ais.setLimit(i);
//        } else {
//            ais.setLimit(null);
//        }
//
//        // Projection
//        ais.setProjection(normalizeProjection(firstNonNull(raw.get("projection"), raw.get("AISProjection"))));
//
//        return ais;
    }
    private String preprocess(String raw) {
        String s = raw;
        s = s.replaceAll("(?s)<think>.*?</think>", "");
        s = s.replaceAll("(?s)<analysis>.*?</analysis>", "");
        // Remove opening ``` or ```json
        s = s.replaceAll("(?m)^```\\s*json\\s*$", "");
        s = s.replaceAll("(?m)^```\\s*$", "");

        // Remove closing ```
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

        // Case 2: raw object
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        throw new IllegalStateException("No JSON object found");
    }

    /**
     * Normalize fact node
     */
    private AISFact normalizeFact(Object node) {
        if (node instanceof Map<?, ?> map) {
            return new AISFact(AISFactNode.OBSERVATION, "count");
        }

        if (node instanceof String s && s.contains("Observation")) {
            return new AISFact(AISFactNode.OBSERVATION, "count");
        }

        throw new IllegalStateException("Unrecognizable AISFact");
    }

    /**
     * Normalize context nodes
     */
    private List<AISContext> normalizeContext(Object node) {
        if (node == null) return List.of();

        List<Map<String, Object>> rawList = castToList(node);
        List<AISContext> result = new ArrayList<>();

        for (Map<String, Object> ctx : rawList) {
            if (ctx.containsKey("type") && ctx.containsKey("filter")) {
                // { "type": "Zone", "filter": {"name":"dmp"} }
                String type = (String) ctx.get("type");
                Map<String, Object> filter = (Map<String, Object>) ctx.get("filter");
                result.add(new AISContext(null, null, null));
            } else if (ctx.containsKey("type")) {
                // { "type":"Zone","name":"dmp" }
                String type = (String) ctx.get("type");
                result.add(new AISContext(null, null, null));
            } else if (ctx.size() == 1) {
                // { "Zone": "dmp" } or { "Month": { "month":1,"year":2024 } }
                String type = ctx.keySet().iterator().next();
                Object value = ctx.get(type);
                if (value instanceof Map<?, ?> mapValue) {
                    result.add(new AISContext(null, null, null));
                } else {
                    result.add(new AISContext(null, null, null));
                }
            } else {
                throw new IllegalStateException("Unknown context shape: " + ctx);
            }
        }
        return result;
    }

    /**
     * Normalize intents
     */
    private List<AISIntent> normalizeIntents(Object node) {
        if (node == null) return List.of();

        List<?> raw = castToList(node);
        List<AISIntent> intents = new ArrayList<>();

        for (Object o : raw) {
            if (o instanceof String s) {
                intents.add(new AISIntent(null, null, null));
            } else if (o instanceof Map<?, ?> m) {
                intents.add(new AISIntent(null,  null, null));
            }
        }

        return intents;
    }

    /**
     * Normalize projections
     */
    private List<AISProjection> normalizeProjection(Object node) {
        if (node == null) return List.of();

        List<?> raw = castToList(node);
        List<AISProjection> result = new ArrayList<>();

        for (Object o : raw) {
            if (o instanceof String s) {
                result.add(null);
            } else if (o instanceof Map<?, ?> m) {
                result.add(null);
            }
        }

        return result;
    }

    /**
     * Utility: get first non-null value
     */
    private Object firstNonNull(Object... objs) {
        for (Object o : objs) if (o != null) return o;
        return null;
    }

    /**
     * Utility: cast object to List<Map<String,Object>>
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castToList(Object node) {
        if (node instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    out.add((Map<String, Object>) map);
                }
            }
            return out;
        }
        throw new IllegalStateException("Cannot cast to list of maps: " + node);
    }
}
