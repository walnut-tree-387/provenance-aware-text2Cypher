package com.example.text2cypher.data.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DominantAttributeSelectionDto extends BaseQueryDto{
    private String dominantDimension; // subtype / zone
//    MATCH (o:Observation)-[:OF_SUBTYPE]->(est:EventSubType)-[:SUBTYPE_OF]->(et:EventType),
//            (o)-[:IN_MONTH]->(m:Month),
//            (o)-[:OBSERVED_IN]->(z:Zone)
//    WHERE et.name = $eventType
//    AND m.year = $year
//    AND z.name = $zone
//    WITH est.name AS subtype,
//    sum(o.count) AS total
//    ORDER BY total DESC
//    LIMIT 1
//    RETURN subtype

}
