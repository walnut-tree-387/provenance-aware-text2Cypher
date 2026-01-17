package com.example.text2cypher.data.cypher;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;

import java.util.*;

public final class CypherResponseMapper {

    private CypherResponseMapper() {}

    public static CypherResponse map(
            List<Record> records,
            String protoNl,
            List<String> nlList,
            String cypher
    ) {

        Map<String, Object> results = new HashMap<>();
        Map<String, ProvenanceRecord> provenanceMap = new LinkedHashMap<>();

        for (int i = 0; i < records.size(); i++) {
            Record record = records.get(i);
            System.out.println(record);
            Object value = unwrap(record.get("answer"));
            Long count = record.containsKey("value")
                    ? record.get("value").asLong()
                    : null;

            results.put("answer-" + i, value);
            if(count != null)results.put("answer-" + i + "_count", count);
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

        return new CypherResponse(
                protoNl,
                results,
                (long) nodeList.size(),
                nodeList,
                nlList,
                cypher
        );
    }
    private static Object unwrap(Value v) {
        if (v == null || v.isNull()) return null;

        return switch (v.type().name()) {
            case "INTEGER" -> v.asLong();
            case "FLOAT"   -> v.asDouble();
            case "BOOLEAN" -> v.asBoolean();
            case "STRING"  -> v.asString();
            case "LIST"    -> v.asList(CypherResponseMapper::unwrapValue);
            case "MAP"     -> v.asMap(CypherResponseMapper::unwrapValue);
            default        -> v.toString();
        };
    }

    private static Object unwrapValue(Value v) {
        return unwrap(v);
    }

}

