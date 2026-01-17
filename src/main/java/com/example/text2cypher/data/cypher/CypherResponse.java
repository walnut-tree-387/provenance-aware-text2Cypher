package com.example.text2cypher.data.cypher;

import java.util.List;
import java.util.Map;

public record CypherResponse(
        String protoNl,
        Map<String, Object> results,
        Long provenanceNodeCount,
        List<ProvenanceRecord> nodeList,
        List<String> nLList,
        String cypher
) {}
