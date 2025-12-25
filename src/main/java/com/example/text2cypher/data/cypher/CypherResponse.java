package com.example.text2cypher.data.cypher;

import java.util.List;

public record CypherResponse(String protoNl, Long result, Long provenanceNodeCount, List<ProvenanceRecord> nodeList) {
}
