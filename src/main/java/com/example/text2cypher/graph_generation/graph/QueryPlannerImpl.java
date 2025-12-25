package com.example.text2cypher.graph_generation.graph;

import com.example.text2cypher.graph_generation.csv_parser.CsvRow;
import com.example.text2cypher.graph_generation.dto.MonthObservation;
import com.example.text2cypher.graph_generation.graph.nodes.*;
import com.example.text2cypher.neo4j.Neo4jService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class QueryPlannerImpl implements QueryPlanner {
    private final Neo4jService neo4jService;
    private static final String queryTemplate = """
            MERGE (z:Zone {name: $zoneName, division: $zoneDivision})
            MERGE (m:Month {code : $monthCode, year: $monthYear, month: $monthMonth, quarter: $monthQuarter})
            MERGE (et:EventType {name: $eventTypeName})
            MERGE (est:EventSubType {name: $subTypeName, severity: $subTypeSeverity})
            MERGE (o:Observation {id: $obsId, count : $obsCount, source : $obsSource})
            MERGE (o)-[:OBSERVED_IN]->(z)
            MERGE (o)-[:IN_MONTH]->(m)
            MERGE (o)-[:OF_SUBTYPE]->(est)
            MERGE (est)-[:SUBTYPE_OF]->(et)
        """;
    private static final Map<Long, String> zoneList = Map.ofEntries(
            Map.entry(1L, "dmp"),
            Map.entry(2L, "cmp"),
            Map.entry(3L, "kmp"),
            Map.entry(4L, "rmp"),
            Map.entry(5L, "bmp"),
            Map.entry(6L, "smp"),
            Map.entry(7L, "rpmp"),
            Map.entry(8L, "gmp"),
            Map.entry(9L, "dhaka_range"),
            Map.entry(10L, "mymensingh_range"),
            Map.entry(11L, "chittagong_range"),
            Map.entry(12L, "sylhet_range"),
            Map.entry(13L, "khulna_range"),
            Map.entry(14L, "barishal_range"),
            Map.entry(15L, "rajshahi_range"),
            Map.entry(16L, "rangpur_range"),
            Map.entry(17L, "ralway_range")
    );

    private static final Map<Long, String> subTypeList = Map.ofEntries(
            Map.entry(1L, "dakoity"),
            Map.entry(2L, "robbery"),
            Map.entry(3L, "murder"),
            Map.entry(4L, "speedy_trial"),
            Map.entry(5L, "riot"),
            Map.entry(6L, "women_and_child_repression"),
            Map.entry(7L, "kidnapping"),
            Map.entry(8L, "police_assault"),
            Map.entry(9L, "burglary"),
            Map.entry(10L, "theft"),
            Map.entry(11L, "other_cases"),
            Map.entry(12L, "arms_act"),
            Map.entry(13L, "explosive_act"),
            Map.entry(14L, "narcotics"),
            Map.entry(15L, "smuggling")
    );



    public QueryPlannerImpl(Neo4jService neo4jService) {
        this.neo4jService = neo4jService;
    }

    @Override
    public Map<String, Object> parseRowToParams(CsvRow csvRow) {
        Zone zone = new Zone();
        zone.set(csvRow.getZone());
        Month month = new Month();
        month.set(csvRow.getMonth());
        Observation observation = new Observation();
        observation.set(csvRow.getCount());
        EventType type = new EventType();
        type.set(csvRow.getType());
        EventSubType subType = new EventSubType();
        subType.set(csvRow.getSubType());
        return getParams(zone, month, type, subType, observation);
    }

    @Override
    public void executeQueries(List<CsvRow> csvRows) {
        csvRows.forEach(csvRow -> {
            neo4jService.executeWithoutResult(queryTemplate, parseRowToParams(csvRow));
        });
    }

    @Override
    public void convertMonthObservation(MonthObservation dto) {
        Long zoneFlag = 1L;
        for (List<Long> monthObservation : dto.getMonthObservations()) {
            for (int j = 0; j < monthObservation.size(); j++) {
                CsvRow csvRow = new CsvRow();
                csvRow.setMonth(dto.getMonth());
                csvRow.setZone(zoneList.get(zoneFlag));
                csvRow.setCount(monthObservation.get(j));
                csvRow.setType(j > 10 ? "recovery" : "crime");
                csvRow.setSubType(subTypeList.get(j + 1L));
                neo4jService.executeWithoutResult(queryTemplate, parseRowToParams(csvRow));
            }
            zoneFlag++;
        }
    }

    private Map<String, Object> getParams(Zone zone, Month month, EventType type, EventSubType subType, Observation observation) {
        System.out.println(zone.toString() +   month.toString() +  type.toString() + subType.toString() + observation.toString());
        observation.setId(zone.getName() + month.getMonth().toString() + subType.getName());
        return Map.ofEntries(
                Map.entry("zoneName", zone.getName()),
                Map.entry("zoneDivision", zone.getDivision()),
                Map.entry("monthCode", month.getCode()),
                Map.entry("monthYear", month.getYear()),
                Map.entry("monthMonth", month.getMonth()),
                Map.entry("monthQuarter", month.getQuarter()),
                Map.entry("eventTypeName", type.getName()),
                Map.entry("subTypeName", subType.getName()),
                Map.entry("subTypeSeverity", subType.getSeverity()),
                Map.entry("obsCount", observation.getCount()),
                Map.entry("obsId", observation.getId()),
                Map.entry("obsSource", observation.getSource())
        );
    }
}
