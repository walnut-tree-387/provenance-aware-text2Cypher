package com.example.text2cypher.data.cypher;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;

import java.util.*;

public final class OlapCypherResponseMapper {
    private OlapCypherResponseMapper() {}
    public static OlapCypherResponse map(
            List<Record> records, List<String> returnClauses
    ){
        List<List<Map<String, Object>>> results = new ArrayList<>();
        Map<String, ProvenanceRecord> provenanceMap = new LinkedHashMap<>();
        for (Record record : records) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String returnClause : returnClauses) {
                if (record.containsKey(returnClause)) {
                    Object value = record.get(returnClause).asObject();
                    row.put(returnClause, value);
                }
            }
            results.add(List.of(row));
            if (record.containsKey("provenance")) {
                record.get("provenance").asList(Value::asNode)
                        .forEach(n -> provenanceMap.putIfAbsent(
                                n.get("id").asString(),
                                new ProvenanceRecord(
                                        n.get("count").asLong(),
                                        n.get("id").asString(),
                                        n.get("source").asString()
                                )
                        ));
            }
        }
        List<ProvenanceRecord> nodeList =
                new ArrayList<>(provenanceMap.values());
        return new OlapCypherResponse(
                results, (long) nodeList.size(), nodeList
        );
    }
}
