package com.example.text2cypher.cypher_benchmark.gold_data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
public interface GoldEntryRepository extends JpaRepository<GoldEntry, Long> {
    Optional<GoldEntry> findFirstByProcessedFalseOrderByIdAsc();
}
