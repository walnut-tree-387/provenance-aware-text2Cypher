package com.example.text2cypher.graph_generation.csv_parser;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CsvParser {
    List<CsvRow> parse(MultipartFile file);
}
