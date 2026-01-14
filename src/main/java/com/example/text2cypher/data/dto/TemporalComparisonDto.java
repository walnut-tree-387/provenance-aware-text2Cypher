package com.example.text2cypher.data.dto;

public class TemporalComparisonDto {
//    MATCH (o:Observation)-[:OF_SUBTYPE]->(est:EventSubType)-[:SUBTYPE_OF]->(et:EventType),
//            (o)-[:IN_MONTH]->(m:Month)
//    WHERE et.name = 'crime'
//    AND m.month IN [1, 2] AND m.year = 2024
//    WITH est.name AS subtype,
//    m.month AS month,
//    sum(o.count) AS total
//    WITH subtype,
//    sum(CASE WHEN month = 1 THEN total ELSE 0 END) -
//    sum(CASE WHEN month = 2 THEN total ELSE 0 END) AS delta
//    RETURN subtype, delta
//    ORDER BY delta DESC
//    LIMIT 5
private Long fromYear;
    private Long toYear;
    private String compareDimension;
}
