package com.example.text2cypher.cypher_benchmark.paraphraser;

import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import org.springframework.stereotype.Component;

import static com.example.text2cypher.ais_evaluation.utils.AISPromptBuilder.getGraphSchemaForAIS;
@Component
public class ParaphrasingPromptBuilder {
    public String buildParaphrasePrompt(QueryType queryType, String protoNL) {
        return """
    You will be acting as a crime analyst and will ask analytical questions about a specific crime graph based on crime statistic recorded By Bangladesh Police.
    First take detail look on the original schema of our crime graph built on top of monthly crime records published by bangladesh police over the 7 year span from 2019 to 2025.
    Graph schema : %s
    
    Based on this graph You will be given:
    1. A prototype natural language question generated from a manually annotated Generalized Query Pattern (GQP).
    2. The query type category.
    Prototype Query: %s
    Query Type : %s
    
    Task: Your task is to rewrite the prototype query into a natural, realistic analytical question that a human analyst might ask
    while examining a collection of monthly crime statistics PDF report over the 7 year period from 2019 to 2025.
    
    TYPE-SPECIFIC INSTRUCTIONS:
    
    If Query Type = COUNT:
    - Ask directly about number of incidents.
    - Do NOT introduce aggregation beyond the specified dimensions.
    - Keep cardinality at the leaf level.
    - Imply answer as only a single count
    
    If Query Type = AGGREGATION:
    - Frame as total, overall, cumulative, or combined count.
    - Clearly imply collapsing over one or more dimensions.
    - Avoid ranking or comparison unless present.
    - Imply Answer as the single count of aggregation.
    
    If Query Type = DOMINANT_ATTRIBUTION:
    - Ask which entity had the highest or most significant contribution.
    - Emphasize dominance under given constraints.
    - Only one dominant entity should be implied.
    
    If Query Type = RANKING:
    - Clearly request an ordered list.
    - Use phrasing like “rank”, “order from highest to lowest”.
    - Maintain filtering constraints.
    - Ranking should imply multiple answers with respective measures
    
    If Query Type = RATIO:
    - Express proportional or relative measure clearly.
    - Use terms like “average”, “proportion”, “share”, “rate”.
    - Ensure both aggregated quantities are implied.
    
    If Query Type = DIFFERENCE:
    - Explicitly express comparison between two aggregated values.
    - Use phrases like “difference between”, “how much higher/lower”.
    - Maintain both comparative dimensions.
    
    If Query Type = BOOLEAN:
    - The output must be answerable with Yes/No.
    - Phrase as a factual condition check.
    - Preserve aggregated condition exactly.
    
    If Query Type = PRIORITY_ORDER:
    - Emphasize explicit ordering criteria.
    - Use phrases like “prioritized based on”.
    - Maintain defined ranking rule.
    
    Strict Global Rules to follow for writing question:
    - Paraphrase Keywords like MONTH, MONTH_YEAR, MONTH_QUARTER, MONTH_CODE, ZONE_NAME, ZONE_DIVISION, EVENT_TYPE, EVENT_SUBTYPE, EVENT_SUBTYPE_SEVERITY into the final question.
    - Preserve the full analytical intent exactly — no change in meaning.
    - The question must be self-contained.
    - Keep all dimensional references (e.g., year, month, zone, subtype) intact.
    - Do NOT add, remove, or change any constraints
    - Do NOT introduce new entities, relations, or conditions
    - Do NOT mention database, schema, nodes, or relationships
    - Use clear and natural phrasing
    - Preserve all aggregation logic.
    - Preserve all comparison, ranking, ratio, or difference semantics.
    - Do NOT introduce assumptions not present in the prototype.
    - The output must be a single question sentence. Anything else but the question will be considered as wrong answer and will be penalized
    
    Output Question:
    """.formatted(getGraphSchemaForAIS(), protoNL, queryType);
    }
}
