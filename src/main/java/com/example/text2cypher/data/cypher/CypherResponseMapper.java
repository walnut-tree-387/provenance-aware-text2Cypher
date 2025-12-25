package com.example.text2cypher.data.cypher;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;

import java.util.List;

public final class CypherResponseMapper {
    private CypherResponseMapper() {}

    public static CypherResponse map(Record record, String protoNl) {
        Long total = record.get("value").asLong();

        List<ProvenanceRecord> nodeList =
                record.get("provenance").asList(v -> {
                    Node n = v.asNode();
                    return new ProvenanceRecord(
                            n.get("count").asLong(),
                            n.get("id").asString(),
                            n.get("source").asString()
                    );
                });

        return new CypherResponse(protoNl, total, (long) nodeList.size(), nodeList);
    }
}
