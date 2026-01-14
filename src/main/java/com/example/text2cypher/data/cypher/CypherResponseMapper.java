package com.example.text2cypher.data.cypher;

import org.neo4j.driver.Record;
import java.util.*;

public final class CypherResponseMapper {

    private CypherResponseMapper() {}

    public static CypherResponse map(
            List<Record> records,
            String protoNl,
            List<String> nlList
    ) {

        List<QueryResultItem> results = new ArrayList<>();
        Map<String, ProvenanceRecord> provenanceMap = new LinkedHashMap<>();

        for (Record record : records) {
            String value = record.get("value").asString();
            Long count = record.containsKey("total")
                    ? record.get("total").asLong()
                    : null;

            results.add(new QueryResultItem(value, count));

            if (record.containsKey("provenance")) {
                record.get("provenance").asList(v -> v.asNode())
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

        return new CypherResponse(
                protoNl,
                results,
                (long) nodeList.size(),
                nodeList,
                nlList
        );
    }
}

