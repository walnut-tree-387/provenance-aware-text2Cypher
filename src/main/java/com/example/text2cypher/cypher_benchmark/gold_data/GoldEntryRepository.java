package com.example.text2cypher.cypher_benchmark.gold_data;

import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
public interface GoldEntryRepository extends JpaRepository<GoldEntry, Long> {
    Optional<GoldEntry> findFirstByProcessedFalseOrderByIdAsc();
    @Query("""
        SELECT g
        FROM GoldEntry g
        WHERE g.processed = false
        AND g.queryType = :queryType
        ORDER BY function('RANDOM')
    """)
    List<GoldEntry> findRandomUnprocessedByQueryType(
            @Param("queryType") QueryType queryType,
            Pageable pageable
    );
    long countByQueryTypeAndProcessedTrue(QueryType queryType);
    @Query("""
        SELECT g
        FROM GoldEntry g
        WHERE g.queryType = :queryType
        ORDER BY function('RANDOM')
    """)
    List<GoldEntry> findRandom200GoldEntry(QueryType queryType, Pageable pageable);
    List<GoldEntry> findAllByQueryType(QueryType queryType);
}
