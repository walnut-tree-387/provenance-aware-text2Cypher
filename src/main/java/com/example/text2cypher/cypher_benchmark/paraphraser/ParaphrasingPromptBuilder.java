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
    Example : How many dacoity crime events recorded by dhaka range police unit during september 2025?
    
    If Query Type = AGGREGATION:
    - Frame as total, overall, cumulative, or combined count.
    - Clearly imply collapsing over one or more dimensions.
    - Avoid ranking or comparison unless present.
    - Imply Answer as the single count of aggregation.
    Example : Calculate the total number of nationwide riot recorded during the post revolution period(July 2025 to November 2025)  in Bangladesh.
    
    If Query Type = DOMINANT_ATTRIBUTION:
    - Ask which entity had the highest or most significant contribution.
    - Emphasize dominance under given constraints.
    - Only one dominant entity should be implied.
    Example : Which zone had the third lowest crime severity overall in whole bangladesh?
    
    If Query Type = RANKING:
    - Clearly request an ordered list.
    - Orders Intents must be mentioned in the output question for PRIORITY_ORDER query type, or else output will be penalized.
    - Use phrasing like “rank”, “order from highest to lowest”.
    - Maintain filtering constraints.
    - Ranking should imply multiple answers with respective measures
    Example : Rank the recovery subtypes in the DMP zone for September 2025 by their total severity and list the top two subtypes along with their severity scores.
    
    If Query Type = RATIO:
    - Express proportional or relative measure clearly.
    - Use terms like “average”, “proportion”, “share”, “rate”.
    - Ensure both aggregated quantities are implied.
    Example : What percentage of the recovery observations recorded in 2025 by the DMP police unit were for narcotics?
    
    If Query Type = DIFFERENCE:
    - Explicitly express comparison between two aggregated values.
    - Use phrases like “difference between”, “how much higher/lower”.
    - Maintain both comparative dimensions.
    Example : Find the difference between crime and recovery incidents count recorded in the second quarter of 2020 by the police units of Dhaka Division?
    
    If Query Type = BOOLEAN:
    - The output must be answerable with Yes/No.
    - Phrase as a factual condition check.
    - Preserve aggregated condition exactly.
    Example : In Q2 2025, Did DMP reported any crime with maximum severity level?
    
    If Query Type = PRIORITY_ORDER:
    - Orders Intents must be mentioned in the output question for PRIORITY_ORDER query type, or else output will be penalized.
    - Emphasize explicit ordering criteria.
    - Use phrases like “prioritized based on”.
    - Maintain defined ranking rule.
    - Priority order should imply multiple answers with respective ordering clauses.
    Example 1 : In November 2025, which police zone showed the weakest performance in terms of both raw recovery cases and severity-adjusted recovery, indicating not only fewer recoveries but also lower impact in curbing serious crimes?
    Example 2 : In 2024, which police zone experienced relatively fewer total crimes yet a higher average crime severity, pointing to a concerning trend where less frequent but more serious offenses dominated its crime profile?
    
    MEASURE SEMANTIC DEFINITIONS (CRITICAL):
    - COUNT_SUM = total number of incidents.
    - WEIGHTED_SUM = total severity score (NOT average severity).
    - Average severity MUST ONLY be used when explicitly defined as a ratio between WEIGHTED_SUM and COUNT_SUM.
    - If only WEIGHTED_SUM appears, it MUST be expressed as "total severity" or "total severity score".
    - NEVER interpret WEIGHTED_SUM alone as average severity.

    
    Strict Global Rules to follow for writing question:
    - Your output must be a single question and nothing but the question. DO NOT add model reasoning in output OR else output will be counted as wrong answer.
    - Use clear and natural human like phrasing
    - Paraphrase Keywords like MONTH, MONTH_YEAR, MONTH_QUARTER, MONTH_CODE, ZONE_NAME, ZONE_DIVISION, EVENT_TYPE, EVENT_SUBTYPE, EVENT_SUBTYPE_SEVERITY into the final question.
    - Preserve all the filter logic as it is, DO NOT change or innovate some new filters not present in the proto NL.
    - Preserve the full analytical intent exactly — no change in meaning.
    - Do NOT add, remove, or change any constraints
    - Do NOT introduce new entities, relations, or conditions
    - Do NOT mention database, schema, nodes, or relationships
    - Preserve all aggregation logic.
    - Preserve all comparison, ranking, ratio, or difference semantics.
    - Do NOT introduce assumptions not present in the prototype.
    - Any Change in overall meaning will counted as wrong paraphrasing.
    - The output must be a single question sentence. Anything else but the question will be considered as wrong answer and will be penalized.
    
    Output Question:
    """.formatted(getGraphSchemaForAIS(), protoNL, queryType);
    }
}
