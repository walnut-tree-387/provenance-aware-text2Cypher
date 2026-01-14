package com.example.text2cypher.paraphrasing.service;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {
    public String buildPrompt(String protoNL) {
        return """
    You are an expert in rewriting structured prototype queries
    into natural, user-like English questions.

    Task:
    Convert the following prototype natural language query into
    a natural English question that a real user might ask.

    Constraints:
    - Do not Include Keywords like EventType, Zone, EventSubType, SubType, Type into the final question please.
    - Preserve the exact meaning and intent
    - Do NOT add, remove, or change any constraints
    - Do NOT introduce new entities, relations, or conditions
    - Do NOT mention database, schema, nodes, or relationships
    - Use clear and natural phrasing
    - The output must be a single question sentence

    Prototype Query:
    "%s"

    Output:
    """.formatted(protoNL);
    }
}
