package com.example.text2cypher.zero_shot;

import org.springframework.stereotype.Component;

@Component
public class NlToCypherPromptBuilder {
    public String buildPrompt(String question) {
        return """
    You are an expert in generating Neo4j Cypher queries from natural english language questions.
    %s
    %s
    Task: Convert the following natural English question about the mention crime knowledge graph that a real user might ask.
    Constraints:
    - Do not Include INVALID CYPHER keywords. If needed use search tools to ensure about cypher keyword validity.
    - Try to extract the exact meaning and intent of the question and generate constraints accordingly.
    - Do NOT add, remove, or change any constraints.
    - Do NOT introduce new nodes, relations, or properties
    - The output MUST be a single valid Cypher query in plain text.
    - DO NOT use markdown, code blocks, backticks, or language tags (e.g., ```cypher).
    - Output ONLY the Cypher query string and nothing else.

    Natural Language Question:
    "%s"

    Output:
    """.formatted(schema(), cypherWritingRules(), question);
    }
    public static String schema() {
        return """
        You are working with a Bangladeshi crime-specific property graph. Learn the node labels, properties and relations from the given graph schema below :
        Graph Nodes :
        1. label : Observation, Properties : count, id, source.
            - Here id is unique property combined these other nodes property(Zone.name-EventSubType.name-Month.month-Month.year). Example - id: "dmp-arms_act-1-2025"
            - count is a number, that represent crime/recovery count. This is the heart of our graph. Most of the query will come centering this property. Example - count: 12
        2. label: Month, Properties: month, year, quarter, code.
            - Here month is integer value for a given month. Example - 1 is January, 2 is February
            - year is also a integer value for a given year. Example - year: 2025
            - quarter is also a integer value for a given quarter. Example - quarter: 1(First 4 months of the year - Jan, Feb, Mar, Apr)
            - code is the popular acronym used to represent month and year together. Example - code: "2024-01" represents January month of the year 2024.
        3. label: Zone, properties: name, division
            - name represent all the 17 police dept working nationwide. Possible values are: (dmp, cmp, kmp, rmp, bmp, smp, rpmp, gmp, dhaka_range, mymensingh_range, chittagong_range, sylhet_range, khulna_range, barishal_range, rajshahi_range, rangpur_range, ralway_range)
            - division is the mapping of which zone falls inside in which division from the 8 possible division in Bangladesh. Division Mapping followed this rule : case "dmp" : case "dhaka_range" : case "gmp" : this.division = "Dhaka";
                                                                                                                                                                     case "cmp" : case "chittagong_range" : this.division = "Chittagong";
                                                                                                                                                                     case "kmp" : case "khulna_range" : this.division = "Khulna";
                                                                                                                                                                     case "smp" : case "sylhet_range" : this.division = "Sylhet";
                                                                                                                                                                     case "rmp" : case "rajshahi_range" : this.division = "Rajshahi";
                                                                                                                                                                     case "bmp" : case "barishal_range" : this.division = "Barishal";
                                                                                                                                                                     case "rpmp" : case "rangpur_range" : this.division = "Rangpur";
                                                                                                                                                                     case "mymensingh_range" : this.division = "Mymensingh";
                                                                                                                                                                     case "ralway_range" : this.division = "Railway";
        4. label: EventType, Properties : name.
            - name represents a operation Type and has only two value only (crime / recovery)
        5. label: EventSubType, Properties: name, severity.
            - name represents a specific crime or recovery subtype and has the following values : dacoity, murder, robbery, speedy_trial, riot, women_and_child_repression, kidnapping, police_assault, burglary, theft, other_cases, arms_act, explosive_act, narcotics, smuggling.
            - severity is an integer assigned to each eventSubType to denote severity. It follows :[ // Death penalty crime - {murder: 5},
                                                                                                    // Organized / armed - {dacoity, arms_act : 4},
                                                                                                   // Violent - {robbery, explosive_act, narcotics : 3},
                                                                                                  // Medium / disruptive - {riot, kidnapping, police_assault, women_and_child_repression, theft, burglary, smuggling : 2},
                                                                                                 // Administrative / legal - { speedy_trial, other_cases : 1}
        Graph Relations:
        1. label: OBSERVED_IN, use_case : Observation - OBSERVED_IN -> Zone. Example : (o: Observation{count: 12}) - OBSERVED_IN -> (z:Zone{name: 'dmp'})
        2. label: IN_MONTH, use_case : Observation - IN_MONTH -> Month. Example : (o: Observation{count: 12}) - IN_MONTH -> (m:Month{month: 1, year: 2024, quarter: 1, code: "2024-01"})
        3. label: OF_SUBTYPE, use_case : Observation - OF_SUBTYPE -> EventSubType. Example : (o: Observation{count: 12}) - OF_SUBTYPE -> (est:EventSubType{name: "dacoity", severity: 4})
        4. label: SUBTYPE_OF, use_case : EventSubType - SUBTYPE_OF -> EventType. Example : (est:EventSubType{name: "dacoity", severity: 4}) - SUBTYPE_OF -> (et:EventType{name: "crime"})
            - Special Mention for the relation SUBTYPE_OF :
                - EventSubType with following name : arms_act, explosive_act, narcotics, smuggling has the SUBTYPE_OF relation with (et:EventType{name: "crime"}).
                - EventSubType with following name :dacoity, murder, robbery, speedy_trial, riot, women_and_child_repression, kidnapping, police_assault, burglary, theft, other_cases has the SUBTYPE_OF relation with (et:EventType{name: "recovery"}).
        """;
    }
    public static String cypherWritingRules(){
        return """ 
        You are an expert in writing cypher query for neo4j. You are an expert in following order of writing to produce a cypher for Neo4j. You will follow a fixed type of cypher writing pattern to produce the accurate cypher. Output cypher will have these following portion concatenated step by step :
        1. MATCH CLAUSES(MUST HAVE) : Start writing the query with this following match clause. For any type question you will use this match clause at First, which includes all of our nodes and relations. Also use these shorthand of node labels (z for zone, o for observation, m for month, et for eventType and est for eventSubType), Don't innovate new short hand or add any other MATCH clause other then these mentioned below:
            MATCH (o:Observation)
            MATCH (o)-[:OF_SUBTYPE]->(est:EventSubType)-[:SUBTYPE_OF]->(et:EventType),
            (o)-[:IN_MONTH]->(m:Month),
            (o)-[:OBSERVED_IN]->(z:Zone)
        2. ADD CONSTRAINTS(MUST HAVE) : In this step you will look for all the known constraints present in the questions. Constraints should be node properties and their value, For example 'dmp zone' is a constraint you can extract z.name = 'dmp', January Month is a constraint you can extract m.month = 1, eventSubType of dacoity is a constraint you can extract est.name = "dacoity", eventType of crime is also a constraint, you can extract et.name = "crime".
           - Each Constraint you build will have two portion
                1. Property signature. For example(z.name, m.code, et.name, est.name, est.severity etc.)
                2. Value. For Example("dmp", '2024-01', "crime", "dacoity", 1, 2024, etc) Replace Month name like January with 1, accurately value from(1-12), for string value use syntactically matched same name.
                3. ADD all the constraints Using 'AND' Keyword. Don't use any other keyword to merge the constraints.
        3. CREATE PROJECTIONS VARIABLES WITH ALIAS(MUST HAVE) : This is a must have step. Don't ignore this step. In this step you will identify what are the resultant variables user have asked for. Depending on the answer type need for the query you will define grouping and aggregate clause with alias to use later part of the query. Use following style -
           - The WITH clause MUST:
             - Contain all grouping keys explicitly
             - Assign aliases to all aggregated expressions
             - Be placed BEFORE the right after the CONSTRAINT CLAUSES
           - Queries that skip this projection step are considered INVALID, even if they appear syntactically executable in Cypher.
           - For example if query ask for only a single count - use o.count AS key
           - if user ask for single zone name - z.name AS key,
           - if it ask for aggregate on node count for sum - sum(o.count) AS total
           - if query required multiple alias name them as key1, key2 etc.
           - if query demands multiple variables for single object alias them as (key1, value1), (key2, value2).
           - add collect(o) as provenance as the default projection alias for every query. Don't forget to add this alias. Its a mandatory projection needed for all the query.
           - Don't use any other alias by yourself, you need to be consistent on alias.
        4. ADD ORDER BY CLAUSE(OPTIONAL) : In this step you will use the alias created in STEP 3(Projection Variables) to define ORDER BY if needed by The query.
        5. ADD LIMIT CLAUSE(OPTIONAL) : In this step you will use LIMIT clause to LIMIT the result to top k(K value can 1 be to k depending on the query)
        6. RETURN CLAUSE(MUST HAVE) : In this step you will return the projection you defined on step 3 and also the ones asked by query. Sometimes some projection variables are needed only for GROUP BY or ORDER BY CLAUSE. So, Pay attention to the projection alias list you defined and add only the ones asked in the query.
            - You will always return the provenance alias. This will help to ensure the provenance of produced query.
        7. DON'T ADD ANY STEP OTHER STEP. ONLY USE THE ABOVE MENTIONED STEPS.
        8. Be consistent with the shorthand of node labels (z for zone, o for observation, m for month, et for eventType and est for eventSubType). DON'T INNOVATE ANY OTHER SHORTHAND BY YOURSELF.
        9. PAY CLOSE ATTENTION TO THE QUERY QUESTION. CORRECTLY IDENTIFY WHAT IS THE RESULT ASKED FOR?, WHAT SHOULD BE THE ALIAS? DO YOU NEED ANY GROUP BY ALIAS? DO YOU TO USE AGGREGATE ALIAS? IS ORDER BY OR GROUP BY OR LIMIT CLAUSE NEEDED? WHAT SHOULD RETURN CLAUSES?
        10. CORRECTLY SPELL THE NODE LEVEL AND NODE PROPERTIES WHILE WRITING THE CYPHER. ALSO CYPHER NEED TO BE ENSURED HAVE THE 'MUST HAVE' PARTS.
        """;
    }
}
