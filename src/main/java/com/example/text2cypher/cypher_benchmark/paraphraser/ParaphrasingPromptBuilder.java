package com.example.text2cypher.cypher_benchmark.paraphraser;

import org.springframework.stereotype.Component;

import static com.example.text2cypher.ais_evaluation.AISPromptBuilder.getGraphSchemaForAIS;
@Component
public class ParaphrasingPromptBuilder {
    public String buildParaphrasePrompt(String protoNL) {
        return """
    You will be acting as a crime analyst and will ask analytical questions about a specific crime graph based on crime statistic recorded By Bangladesh Police.
    Graph Schema: "%s"
    Task:
    Convert the following prototype natural language query into a natural English question that a real user might ask.
    Prototype Query: %s
    Constraints:
    - Paraphrase Keywords like MONTH, MONTH_YEAR, MONTH_QUARTER, MONTH_CODE, ZONE_NAME, ZONE_DIVISION, EVENT_TYPE, EVENT_SUBTYPE, EVENT_SUBTYPE_SEVERITY into the final question.
    - Preserve the exact meaning and intent
    - Do NOT add, remove, or change any constraints
    - Do NOT introduce new entities, relations, or conditions
    - Do NOT mention database, schema, nodes, or relationships
    - Use clear and natural phrasing
    - The output must be a single question sentence
    Output:
    """.formatted(getGraphSchemaForAIS(), protoNL);
    }
}
