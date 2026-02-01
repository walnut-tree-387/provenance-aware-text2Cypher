package com.example.text2cypher.zero_shot;

import org.springframework.stereotype.Component;

@Component
public class NlToCypherPromptBuilder {
    public String buildAISPrompt(String question) {
        return """
            You are an expert in understanding analytical questions over a Crime Knowledge Graph and converting them into a Structured AIS Object in JSON Format.
            %s
            %s
           Task:
           Convert the following natural English question into a single AIS object in a valid json format.
           Answer with markdown fences(```json OR ```) will be counted as wrong answer.
            Constraints:
            - Do NOT generate Cypher.
            - Do NOT include explanations or comments.
            - Do NOT invent Nodes, properties, or relationships.
            - Use ONLY concepts present in the provided graph schema.
            - AIS must reflect the exact analytical intent of the question.
            - Output MUST be a single AIS object and nothing else.
            Natural Language Question:
            "%s"
            Output:
           """.formatted(getGraphSchemaForAIS(), aisWritingRules(), question);
    }
    public static String aisWritingRules() {
        return """
        You are an expert in finding correct Analytical Intent Specification Semantics (AIS) from a given Natural Language questions over a Crime Knowledge Graph.
        AIS is a pure semantic contract. It MUST be independent of Cypher syntax and MUST be derivable purely from the user question.
        You must ONLY extract AIS semantics that is explicitly or implicitly present in the question
        %s
        %s
        %s
        %s
        %s
        %s
        %s
        %s
        """.formatted(generateAISClassPrompt(), getFactPrompt(), getContextPrompt(),
                getAxesPrompt(), getIntentPrompt(), getDerivedIntentPrompts(), getOrderAndLimitAndOffsetPrompt(), getProjectionPrompt());
    }
    public static String getProjectionPrompt(){
        return """
        9. PROJECTION
        Projection specifies which previously computed elements constitute the final answer to the question. Projection represents WHAT is returned, not HOW it is computed.
        Rules:
        1. Projection MUST be populated.
        2. Projection elements MUST be selected only from the aliases that have already been defined in : AXIS, INTENT, DERIVED_INTENT sections of AIS.
        2. Projection MUST NOT introduce any new computation, new context, new aggregation or derivation.
        3. Projection MUST include ONLY alias that are necessary and sufficient to answer the question and explicitly asked.
        4. If a single entity was required by the question, do not include an intermediate alias that was calculated only to reach the final result.
        6. Order Intent aliases MUST be added to the projection.
        5. DO NOT Include sections(AXIS, INTENT, DERIVED_INTENT) in projection, add the alias only. Else answer will be penalized.
        """;
    }
    public static String getOrderAndLimitAndOffsetPrompt(){
        return """
        ----------------------------------------
        6. ORDER INTENT
        ----------------------------------------
        OrderIntent defines how results should be ranked or sorted.
        OrderIntent is OPTIONAL and MUST be populated ONLY when the question explicitly implies ordering. There could be multiple order intents in a single question.
        Rules:
        1. Populate OrderIntent ONLY if the question includes ranking keywords:
           - "highest", "most", "maximum"
           - "lowest", "least", "minimum"
           - "top K", "bottom K", "ranked by"
        2. OrderIntent MUST reference a computed result mentioned in INTENT and DERIVED_INTENT sections, not a raw dimension.
           - The 'by' field is a string that MUST exactly match(STRING EXACT MATCH) with one of the name of Intent OR derivedIntent
           - The 'direction' field value can be → ASC or DESC depending on the question
        3. Ordering NEVER references:
           - raw node properties
           - dimensions (Zone, Month, EventType, etc.)
        4. Direction rules:
           - DESC for: highest, most, top
           - ASC for: lowest, least, bottom
        5. Ordering MUST follow the priority of INTENT OR DERIVED_INTENT asked in the question. Incorrect identification of intent ordering will be penalized.
        ---------------------------------------
        7. LIMIT
        ---------------------------------------
        Limit Implies how many entities to include in the final result.
        Rules:
        - Populate ONLY if the question specifies top-k or highest or lowest
        - Default is NULL.
        Examples:
        - "Top 3 areas" → limit = 3
        - "Highest month" / "Lowest count" → limit = 1
        ------------------------------------------
        8. OFFSET
        ------------------------------------------
        OFFSET specifies how many top-ranked results to skip before returning the final answer
        Rules:
        - OFFSET applies only to ranked results (ORDER BY).
        - Use OFFSET ONLY when the question implies skipping the top k results (e.g., "2nd highest", "after top 3").
        - Populate ONLY if the question specifies k-highest or k-lowest
        - OFFSET MUST NOT be used for extreme-only queries (highest / lowest). Default is NULL.
        - OFFSET MUST BE used with LIMIT value
        - OFFSET MUST BE <= LIMIT
        Examples:
        - "second highest" → offset = 1
        - "third lowest" → limit = 2
        """;
    }
    public static String getDerivedIntentPrompts(){
        return """
        5. DERIVED INTENTS
        AISDerivedIntent represents different algebraic calculations that can be performed on already-computed AISIntents.
        Each AISDerivedIntent MUST contain:
        - name: unique identifier of the derived value, used in projection.
        - type: one of AISDerivedType
        - operands: list of AISIntent names used in the computation
        
        Possible Derived Intent Types:
        1. DIFFERENCE
        - Absolute difference between TWO explicitly defined operands.
        - Measures how much one quantity exceeds or falls short of another.
        When to use DIFFERENCE:
        - Question uses phrases such as:  "difference between", "gap", "how much more", "how much less", "compare X and Y"
        Examples:
        - "Difference between crimes and recoveries in 2024"
        - "How much higher were murders than robberies in DMP?"
        2. GT, LT, EQ :
        - These operators are mostly used to determine a boolean answer.
        - Use these operators for comparing two previously defined operands in the intent section.
        - Use these operators Comparing a single intent against a constant value.
        - operands section MUST have only 2 operands that are being compared in this derived intent.
        - Example : is dmpTotalCount greater than cmpTotalCount?, is dmpMurderCount greater than 0?, Did any murder occur?
        3. RATIO
        - Relative comparison between TWO explicitly specified operands.
        - The question implies a recognized canonical ratio (e.g. averages, rates)
        When to use RATIO:
        - Question uses phrases such as:  ‘percentage’, ‘share’, ‘portions’, 'rate'
        Examples:
        - ‘Ratio of crimes to recoveries’
        - “What percentage of total crimes was contributed by dacoity?’
        - ‘Average severity of crimes in 2024’   ← canonical ratio
        
        Canonical Derived Measure Semantics (CRITICAL)
        Some natural language phrases correspond to STANDARD DERIVED MEASURES. These measures MUST be expressed using existing AISDerivedTypes. The model MUST map these phrases to their canonical algebraic form.
        Phrase triggers:
         - "average severity", "mean severity", "average crime severity", "severity on average"
        Semantic meaning:
         - Average severity = total severity weight divided by total event count
        Required operands:
         - ONE AISIntent of type SEVERITY_WEIGHTED_COUNT
         - ONE AISIntent of type TOTAL_COUNT
        DerivedIntent representation:
         - type: RATIO
         - operands: [severityWeightedIntent, totalCountIntent]
        Rules:
         - The question DOES NOT need to explicitly mention both operands.
         - If "average severity" is requested, the model MUST infer this ratio.
         - If required operands are missing, they MUST be generated as AISIntents.
        
        GENERAL RULES
        1. DerivedIntent operands MUST reference existing AISIntent names.
        2. DerivedIntent MUST NOT define context or filters.
        3. Do NOT invent new DerivedIntentTypes.
        4. If a statistical term maps to a known canonical ratio, use RATIO.
        5. If no derivation is explicitly or implicitly requested → DerivedIntents MUST be empty.
        6. If the question asks only for a raw count or total, DerivedIntents MUST be empty.
        """;
    }
    public static String getIntentPrompt(){
        return """
        4. INTENT
        An AISIntent represents the PRIMARY numeric quantity requested by the question.
        Each AISIntent must have:
        - name: a unique identifier (used by DerivedIntent operands and projection)
        - type: one of the AISIntentType
        - localContext: OPTIONAL filters applied ONLY to this intent
        There are only two AISIntentType possible for our graph.
        - TOTAL_COUNT → sum (o.count)
        - SEVERITY_WEIGHTED_COUNT → sum of (o count × est.severity)
        LOCAL CONTEXT (INTENT-SCOPED FILTERS)
        localContext represents ONLY the difference from Global Context needed to distinguish one intent from another.
        It MUST NOT restate any condition already true in Global Context.
        Rules:
        1. An Intent will have a local context if that filter is applicable ONLY AND ONLY for that single intent.
        2. Use localContext when the question compares a subset against a broader set.
        3. Use localContext when different intents require different filters.
        4. Do NOT duplicate global context filters in localContext.
        5. Typical use cases:
          - Subtype vs total
          - One zone vs another
          - One event type vs all events
        6. localContext MUST follow the same AISContext rules as global context.
        7. localContext MUST be an empty list if the intent applies to the entire global slice.
        Examples:
        - "difference of recoveries in Q1 and Q2"
              1.   ‘localContext’ : { dimension : ‘MONTH_QUARTER’ operator : ‘=’, value: 1}
              2.   ‘localContext’ : { dimension : ‘MONTH_QUARTER’ operator : ‘=’, value: 2}
        - "murders in DMP vs CMP" →
              1.	‘localContext’ : { dimension : ‘ZONE_NAME’ operator : ‘=’, value: ‘dmp’}
              2.	localContext’ : { dimension : ‘ZONE_NAME’ operator : ‘=’, value: ‘cmp’}
        AIS Intent Writing Rules:
        1. At least ONE AISIntent must be present.
        2. If the question asks "how many", "total number", or "count" → use TOTAL_COUNT.
        3. If the question asks about "impact", "severity weighted", or "overall seriousness" → use SEVERITY_WEIGHTED_COUNT.
        4. If the question asks for average severity, TOTAL_COUNT AND SEVERITY_WEIGHTED_COUNT is a must have in intent List.
        5. Use localContext ONLY if an intent applies to a different slice than others.
        6. Aggregation differences (TOTAL_COUNT vs SEVERITY_WEIGHTED_COUNT) NEVER justify localContext. localContext is driven ONLY by filtering differences, not aggregation semantics.
        """;
    }
    public static String getContextPrompt(){
        return """
        2. CONTEXT
        Each AISContext must have the following properties:
        - dimension: Possible values - MONTH, MONTH_YEAR, MONTH_QUARTER, MONTH_CODE, ZONE_NAME, ZONE_DIVISION, EVENT_TYPE, EVENT_SUBTYPE, EVENT_SUBTYPE_SEVERITY
        - operator: Possible values - EQ, IN, GT, LT, GTE, LTE, NOT_IN
        - value: you MUST extract from the question. Possible data types - integer, string, or list depending on operator
        
        CONTEXT TYPES : There are 2 types of Context.
        - Global context defines the BASE contexts that are applied for the entire query.
        - Global Context defines the allowed and minimal universe, LocalContext may further specialize or separate operands within that universe.
        - Applying a constraint in Global Context prohibits repeating the same constraint in localContext,
                unless the localContext further narrows or contrasts it (e.g., subtype selection within a global IN set).
        - If a filter applies to all intents, it MUST appear only in Global Context.
                Repeating the same localContext in multiple intents is INVALID, even if intent types differ.
        - LocalContext MUST ONLY be used to express INTENT-SPECIFIC DEVIATIONS from the global context and MUST be listed in INTENT Section.
        - Same local context MUST NOT be applied to different Intent. It MUST be a Global Context.
        - If a question explicitly constrains any DIMENSION to a finite or bounded domain (explicit values, comparisons, ranges, thresholds),
          the planner MUST apply that constraint in Global Context first using the appropriate operator(IN, =, >, >=, <, <=).
        - Example :
            1. Ratio/Difference of narcotics to arms act recoveries / Q1 VS Q2 / January vs December / Crime Vs Recovery / Dhaka Vs Chittagong
               → GlobalContext: { dimension: 'EVENT_SUBTYPE', operator: 'IN' , value : ['narcotics', 'arms_act'] }
               → LocalContext: 1. { dimension: 'EVENT_SUBTYPE', operator: '=' , value : 'narcotics' } 2. { dimension: 'EVENT_SUBTYPE', operator: '=' , value : 'arms_act' }
        This section (CONTEXT) MUST list the global contexts only. LocalContext is prohibited in this section and MUST be added INTENT Section if needed.
        
        Rules:
        1. Every filter mentioned in the question MUST appear as one AISContext object.
        2. Context is purely restrictive. It must NOT include aggregations, counts, projections, or grouping information.
        3. Map explicit MONTH names to integers (e.g., "January" -> 1).
        4. Map MONTH_QUARTER mentions integers 1-4 (e.g., "third quarter" -> 3, "Q2" -> 2).
        5. MONTH and MONTH_YEAR can optionally be combined into MONTH_CODE (e.g., "January 2025" -> "2025-01") OR split into MONTH and YEAR separately.
        6. Map words similar to zone/area/region/division/metropolitan to one of the valid dimensions ZONE_NAME or ZONE_DIVISION.
        7. Value MUST contain a valid string from the schema(ex. ‘dmp’, ‘rpmp’) against ZONE_NAME or ZONE_DIVISION.
           - Invalid values that are not present in the graph schema will be counted as wrong answer
           - if a zone/area name seems unambiguous and DIMENSION can't be decided, the default   strategy is to use ZONE_DIVISION. ZONE_DIVISION covers a broader area.
            Example :
            “Barishal” could mean both 'bmp' or 'barishal_range' ZONE_NAME. But it will always mean ZONE_DIVISION : 'Barishal'
            “MyMensingh area" -> could mean ZONE_NAME : mymensingh_range OR ZONE_DIVISION : Mymensingh,
        8.EVENT_TYPE inference is MANDATORY WHEN EVENT_SUBTYPE IS IN THE CONTEXT.
          - EVENT_SUBTYPE context MUST include EVENT_TYPE context also.
          - Use 'SUBTYPE_OF' Relational Mapping from The Graph Schema section to map an EVENT_SUBTYPE to correct EVENT_TYPE.
          - DO NOT Guess or provide invalid mapping. Failure to infer EVENT_TYPE when EVENT_SUBTYPE exists is invalid extraction.
        Example : EVENT_SUBTYPE: dacoity -> EVENT_TYPE : 'crime', EVENT_SUBTYPE: arms_act -> EVENT_TYPE : 'recovery'
        9. Map EventSubType values exactly as spelled in the schema (e.g., dacoity, arms_act, smuggling, women_and_child_repression). DO NOT Incorrectly spell EventSubType  values.
        10. EVENT_SUBTYPE_SEVERITY is for estimating severity for a crime OR recovery incident.  Map severity to DIMENSION EVENT_SUBTYPE_SEVERITY and It’s value MUST be from 1 (lowest) to 5 (highest).
        11. For multiple values (e.g., "dacoity or robbery"), use operator IN with a list of values.
        12. For ranges of months, quarters, or other numeric values, use operator IN with [all possible values].
        13. Do NOT invent or add extra CONTEXT not present in the question.
        13. Output must be a **JSON array** of objects, where each object represents one AISContext with dimension, operator, and value.
        Examples:
        - "January 2024"
         → Option 1: [{ dimension: MONTH, operator: EQ, value: 1}, {dimension: MONTH_YEAR, operator: EQ, value: 2024}]
         → Option 2: [{ dimension: MONTH_CODE, operator: EQ, value: "2024-01" }]
        
        - "Rangpur division" → [{ dimension: ZONE_DIVISION, operator: EQ, value: "Rangpur" }]
        
        - "arms act incidents" → [{ dimension: EVENT_SUBTYPE, operator: EQ, value: "arms_act" }]
        
        - "lowest severity recovery" :  [{ dimension: EVENT_TYPE, operator: EQ, value: "recovery" }, { dimension: EVENT_SUBTYPE_SEVERITY, operator= EQ, value= 1 }]
        
        - "high severity crime count":
         → Option 1:  [ {dimension: EVENT_TYPE, operator: EQ, value: "crime"},  { dimension: EVENT_SUBTYPE_SEVERITY, OPERATOR = GTE, value= 4} ]
         → Option 2:  [ { dimension: EVENT_TYPE, operator: EQ, value: "crime"}, {dimension: EVENT_SUBTYPE_SEVERITY, operator= IN, value= [4, 5]} ]
        
         "third quarter of 2024" :
         → Option 1: [ { dimension: QUARTER, operator: EQ, VALUE: 3}, { dimension: MONTH_YEAR, operator: EQ, value: 2024} ]
         → Option 2: [ { dimension: MONTH, operator: IN, value: [7, 8, 9]}, { dimension: MONTH_YEAR, operator: EQ, value: 2024 } ]
        """;
    }
    public static String getAxesPrompt(){
        return """
        3. AXES
        Axes define *how the fact is grouped* in the result. They correspond directly to OLAP “group by” dimensions in the graph.
        Properties:
        - Each axes object contains:
        - dimension: one AISDimension value which denotes which dimension result will be grouped into (ZONE_NAME, ZONE_DIVISION, MONTH, MONTH_YEAR, MONTH_QUARTER, MONTH_CODE, EVENT_TYPE, EVENT_SUBTYPE, EVENT_SUBTYPE_SEVERITY)
        - name is an alias string only to be used in projection.
        - Do not invent new property.
        Rules:
        1. Axes exist ONLY if the question asks for a **comparison, ranking, or breakdown**.
        2. If the question asks for a **single scalar value** (e.g., total count for one zone/month) → axes MUST be empty.
        3. Axes dimension property correspond strictly to **graph dimensions** only:
            - Zone → ZONE_NAME or ZONE_DIVISION
            - Time → MONTH, MONTH_YEAR, MONTH_QUARTER, MONTH_CODE
            - Event → EVENT_TYPE, EVENT_SUBTYPE, EVENT_SUBTYPE_SEVERITY
        4. Axes can contain **multiple axis** object for **multi-dimensional aggregation**.
            - The order of axis matters: first axis → primary grouping, second axis → secondary grouping, etc.
        5. Do NOT add axes with dimensions that are not explicitly asked for in the question.
        Examples:
        **Single-axis examples:**
        1. Which month in 2025 had the highest number of murders?
            - Axis: [ {dimension: 'MONTH', name: 'month'} ] → grouping result by month
        2. Top 3 areas with the most arms act incidents in Q3 2024
            - Axis: [ {dimension: ‘ZONE_NAME’, name: 'zone_name'} ] → grouping result by zone for ranking
        **Multi-axis examples:**
        1. Total recoveries per Zone and Month in 2025
            - Axis: [ {dimension: 'ZONE_NAME', name: 'zone_name'}, {dimension: 'MONTH', name: 'month'} ] → 2D aggregation table:
        2. Total arms act incidents by Zone and EventSubType in January 2025
            - Axis: [ {dimension: 'ZONE_NAME', name: 'zone_name'}, {dimension: 'EVENT_SUBTYPE', name: 'subtype_name'} ] → grouped by both Zone and EventSubType
        """;
    }
    public static String generateAISClassPrompt(){
        return """
            AIS OBJECT STRUCTURE (MANDATORY CONTRACT) :
               AIS {
                   AISFact fact;
                   List<AISContext> context;
                   List<AISAxis> axes;
                   List<AISIntent> intents;
                   List<AISDerivedIntent> derivedIntents;
                   List<AISOrderIntent> orderIntents;
                   Integer limit;
                   Integer offset;
                   List<AISProjection> projection;
               }
               AIS OBJECT KEY NAMING RULES (MANDATORY):
               1. MUST use **these exact property names** in the JSON output:
               - fact
               - context
               - axes
               - intents
               - derivedIntents
               - orderIntents
               - limit
               - offset
               - projection
               2. Do not invent any new key by yourself, You MUST use these exact keys.
               3. Maintain **case-sensitivity** exactly as above (camelCase).
               4. All keys MUST appear in the output JSON, even if empty list or null.
               5. JSON MUST be valid and parseable. Start with `{`, end with `}`. **No markdown, fences, backticks, or extra text.**
               6. Example output:
               {
                   "fact": { "node": "Observation", "field": "count" },
                   "context": [
                       { "dimension": "MONTH_CODE", "operator": "EQ", "value": '2024-01' }
                   ],
                   "axes": [{ dimension: 'ZONE_NAME', name: 'zone_name' }],
                   "intents": [{ name : 'observations', type : 'TOTAL_COUNT', localContext: []}],
                   "derivedIntents": [],
                   "orderIntents": [],
                   "limit": 1,
                   "offset": null,
                   "projection": [ 'zone_name', 'observations' ]
                   }
               }
        """;
    }
    public static String getFactPrompt(){
        return """
        1. FACT
        The FACT represents the primary observable being analyzed. For our graph Observation is the first class node and count is the property we are interested on.
        Rules:
        - always produce "fact" : {"node" : Observation, "field" : count}
        - don't innovate anything other than "fact" : {"node" : Observation, "field" : count}. Or it will be counted as a wrong output.
        """;
    }
    public static String getGraphSchemaForAIS(){
        return """
                You are working with a Bangladeshi crime-specific property graph. Learn the node labels, properties and relations from the given graph schema below :
                Graph Nodes :
                1. Label : Observation, Properties : count, id, source.
                   - Here id is a unique property combined with these other nodes properties(Zone.name-EventSubType.name-Month.month-Month.year).\s
                Example - id: "dmp-arms_act-1-2025"
                   - count is a number that represents crime/recovery count. This is the heart of our graph. Most of the query will come centering this property. Example - count: 12
                2. Label: Month, Properties: MONTH, MONTH_YEAR, MONTH_QUARTER, MONTH_CODE.
                   - MONTH is the integer value for a given month. Example - 1 is January, 2 is February
                   - MONTH_YEAR is also an integer value for a given year. Example - year: 2025
                   - MONTH_QUARTER is an integer value for a given quarter. Example - 1(First 3 months of the year - Jan, Feb, Mar)
                   - MONTH_CODE is the code representing month and year together. Example - "2024-01" represents January month of the year 2024.
                3. Label: Zone, properties: ZONE_NAME, ZONE_DIVISION
                   - ZONE_NAME represents all the 17 police zones. Possible values are: (dmp, cmp, kmp, rmp, bmp, smp, rpmp, gmp, dhaka_range, mymensingh_range, chittagong_range, sylhet_range, khulna_range, barishal_range, rajshahi_range, rangpur_range, ralway_range)
                   - ZONE_DIVISION is the mapping for a police zone into a division.
                    Division Mapping followed this rule :
                    - 	"dmp",  "dhaka_range" , "gmp"  : ZONE_DIVISION  = "Dhaka"                                                                                                                                                       -	 "cmp",  "chittagong_range" : ZONE_DIVISION = "Chittagong"                                                                                                                              -	"kmp", "khulna_range" : ZONE_DIVISION = "Khulna";                                                                                                                             - 	"smp", "sylhet_range" : ZONE_DIVISION = "Sylhet";                                                                                                                      -	"rmp", "rajshahi_range" : ZONE_DIVISION = "Rajshahi";                                                                                                                                -	"bmp",  "barishal_range" : ZONE_DIVISION = "Barishal";                                                                                                                                               -	"rpmp", "rangpur_range" : ZONE_DIVISION = "Rangpur";
                    -	"mymensingh_range" : ZONE_DIVISION = "Mymensingh";
                    -	 "ralway_range" : ZONE_DIVISION = "Railway";
                4. Label: EventType, Properties : EVENT_TYPE.
                   - EVENT_TYPE represents a operation Type and has only two value only (crime / recovery)
                5. Label: EventSubType, Properties: EVENT_SUBTYPE, EVENT_SUBTYPE_SEVERITY
                   - EventSubType represents a specific crime or recovery subtype and has the following values
                    (dacoity, murder, robbery, speedy_trial, riot, women_and_child_repression, kidnapping, police_assault, burglary, theft, other_cases, arms_act, explosive_act, narcotics, smuggling)
                   - EVENT_SUBTYPE_SEVERITY is an integer assigned to each eventSubType to denote its severity.
                            - murder: 5,
                            - dacoity, arms_act : 4,
                            - robbery, explosive_act, narcotics : 3,
                            - riot, kidnapping, police_assault, women_and_child_repression, theft, burglary, smuggling : 2,
                            - speedy_trial, other_cases : 1
                Graph Relations:
                1. Label: OBSERVED_IN, use_case : Observation - OBSERVED_IN -> Zone.
                 Example : (o: Observation) - OBSERVED_IN -> (z:Zone)
                2. Label: IN_MONTH, use_case : Observation - IN_MONTH -> Month.
                 Example : (o: Observation) - IN_MONTH -> (m:Month)
                3. Label: OF_SUBTYPE, use_case : Observation - OF_SUBTYPE -> EventSubType.
                Example : (o: Observation) - OF_SUBTYPE -> (est:EventSubType)
                4. Label: SUBTYPE_OF, use_case : EventSubType - SUBTYPE_OF -> EventType.
                Example : (est:EventSubType) - SUBTYPE_OF -> (et:EventType)
                - Special Mention for the relation SUBTYPE_OF :
                       - EventSubType with following name : arms_act, explosive_act, narcotics, smuggling has the SUBTYPE_OF relation with EVENT_TYPE: "recovery".
                       - EventSubType with following name :dacoity, murder, robbery, speedy_trial, riot, women_and_child_repression, kidnapping, police_assault, burglary, theft, other_cases has the SUBTYPE_OF relation with EVENT_SUBTYPE : "crime"
        """;
    }
    public static String previousSchema() {
        return """
        You are working with a Bangladeshi crime-specific property graph. Learn the node labels, properties and relations from the given graph schema below :
        Graph Nodes :
        1. label : Observation, Properties : count, id, source.
            - Here id is unique property combined these other nodes property(Zone.name-EventSubType.name-Month.month-Month.year). Example - id: "dmp-arms_act-1-2025"
            - count is a number, that represent crime/recovery count. This is the heart of our graph. Most of the query will come centering this property. Example - count: 12
        2. label: Month, Properties: month, year, quarter, code.
            - Here month is integer value for a given month. Example - 1 is January, 2 is February
            - year is also a integer value for a given year. Example - year: 2025
            - quarter is also a integer value for a given quarter. Example - quarter: 1(First 3 months of the year - Jan, Feb, Mar)
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
    """.formatted(previousSchema(), cypherWritingRules(), question);
    }
}
