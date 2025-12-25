package com.example.text2cypher.graph_generation;

import com.example.text2cypher.graph_generation.csv_parser.CsvParser;
import com.example.text2cypher.graph_generation.csv_parser.CsvRow;
import com.example.text2cypher.graph_generation.dto.MonthObservation;
import com.example.text2cypher.graph_generation.graph.QueryPlanner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class UploadController {
    private final CsvParser csvParser;
    private final QueryPlanner queryPlanner;

    public UploadController(CsvParser csvParser, QueryPlanner queryPlanner) {
        this.csvParser = csvParser;
        this.queryPlanner = queryPlanner;
    }

    @PostMapping("/upload/complete-csv")
    public ResponseEntity<?> uploadCompleteCsv(@RequestParam("file") MultipartFile file) {
        List<CsvRow> csvRows = csvParser.parse(file);
        queryPlanner.executeQueries(csvRows);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/month-data")
    public ResponseEntity<?> updateMonthData(@RequestBody MonthObservation monthObservations) {
        queryPlanner.convertMonthObservation(monthObservations);
        return ResponseEntity.ok().build();
    }
}
