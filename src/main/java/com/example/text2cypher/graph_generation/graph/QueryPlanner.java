package com.example.text2cypher.graph_generation.graph;

import com.example.text2cypher.graph_generation.csv_parser.CsvRow;
import com.example.text2cypher.graph_generation.dto.MonthObservation;

import java.util.List;
import java.util.Map;

public interface QueryPlanner {
    Map<String, Object> parseRowToParams(CsvRow csvRow);
    void executeQueries(List<CsvRow> csvRows);
    void convertMonthObservation(MonthObservation monthObservations);
}
