package com.example.text2cypher.graph_generation.csv_parser;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvParserImpl implements CsvParser {
    @Override
    public List<CsvRow> parse(MultipartFile file) {
        List<CsvRow> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = br.readLine(); // skipping headers
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                CsvRow row = new CsvRow();
                row.setZone(tokens[0].trim());
                row.setMonth(tokens[1].trim());
                row.setType(tokens[2].trim());
                row.setSubType(tokens[3].trim());
                row.setCount(Long.parseLong(tokens[4].trim()));
                rows.add(row);
            }
        }catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return rows;
    }
}
