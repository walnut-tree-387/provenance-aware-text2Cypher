package com.example.text2cypher.data.cypher;

import java.util.List;

public record CypherResponse(
        String protoNl,
        List<QueryResultItem> results,
        Long provenanceNodeCount,
        List<ProvenanceRecord> nodeList,
        List<String> nLList
) {}
