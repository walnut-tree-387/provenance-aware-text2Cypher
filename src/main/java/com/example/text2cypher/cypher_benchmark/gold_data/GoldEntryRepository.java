package com.example.text2cypher.cypher_benchmark.gold_data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public interface GoldEntryRepository extends JpaRepository<GoldEntry, Long> {
    @Query(" SELECT ge from GoldEntry ge WHERE ge.processed = false ")
    List<GoldEntry> findByProcessed();
}
