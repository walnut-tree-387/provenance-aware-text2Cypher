package com.example.text2cypher.webclients.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ChatRequest {
    private String model;
    private List<ClientMessage> messages;
    private double temperature;
    private double top_p;
    private Long max_completion_tokens;
    private boolean stream;

    private Map<String, Object> extraParams = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getExtraParams() {
        return extraParams;
    }

    public void addExtraParam(String key, Object value) {
        this.extraParams.put(key, value);
    }
}
