package com.example.text2cypher.data.cypher;

import java.util.List;
import java.util.Map;

public record OlapCypherResponse(
        List<List<Map<String, Object>>> results,
        Long provenanceNodeCount,
        List<ProvenanceRecord> nodeList
) {}
