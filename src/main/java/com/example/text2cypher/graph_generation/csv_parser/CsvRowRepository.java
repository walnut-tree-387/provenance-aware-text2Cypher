package com.example.text2cypher.graph_generation.csv_parser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CsvRowRepository extends JpaRepository<CsvRow, Long> {
}
