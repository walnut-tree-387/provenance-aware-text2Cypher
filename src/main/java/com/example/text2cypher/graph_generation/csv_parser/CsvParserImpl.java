package com.example.text2cypher.graph_generation.csv_parser;

import com.example.text2cypher.graph_generation.dto.FineTuneData;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
     public void createFineTuneCsv(List<FineTuneData> data) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("train.csv", StandardCharsets.UTF_8))) {
            writer.write("Id,Type,Question,Answer");
            writer.newLine();
            int manualId = 1;
            for (FineTuneData row : data) {
                String line = String.format("%d,%s,\"%s\",\"%s\"",
                        manualId++,
                        row.getQueryType(),
                        row.getNl().replace("\"", "\"\""),
                        row.getAnswer().replace("\"", "\"\""));
                writer.write(line);
                writer.newLine();
            }
            System.out.println("File saved successfully to project root: train.csv" );
        } catch (IOException e) {
            throw new RuntimeException("Failed to save CSV file: " + e.getMessage());
        }
    }
}
