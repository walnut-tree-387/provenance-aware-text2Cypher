package com.example.text2cypher.data;

import com.example.text2cypher.data.cqp.factories.DominantAttributionCqpFactory;
import com.example.text2cypher.data.cqp.factories.RankingCqpFactory;
import com.example.text2cypher.data.cqp.factories.TemporalAggregationCqpFactory;
import com.example.text2cypher.data.cqp.factories.TemporalCountCqpFactory;
import com.example.text2cypher.data.dto.DominantAttributeSelectionDto;
import com.example.text2cypher.data.dto.RankingQueryDto;
import com.example.text2cypher.data.dto.TemporalAggregationRequestDto;
import com.example.text2cypher.data.dto.TemporalCountDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/query-types")
public class QueryTypeController {
    private final TemporalCountCqpFactory temporalCountCqpFactory;
    private final TemporalAggregationCqpFactory temporalAggregationCqpFactory;
    private final DominantAttributionCqpFactory dominantAttributionCqpFactory;
    private final QueryProcessingService queryProcessingService;
    private final RankingCqpFactory rankingCqpFactory;

    public QueryTypeController(TemporalCountCqpFactory temporalCountCqpFactory,
                               TemporalAggregationCqpFactory temporalAggregationCqpFactory,
                               DominantAttributionCqpFactory dominantAttributionCqpFactory, QueryProcessingService queryProcessingService, RankingCqpFactory rankingCqpFactory) {
        this.temporalCountCqpFactory = temporalCountCqpFactory;
        this.temporalAggregationCqpFactory = temporalAggregationCqpFactory;
        this.dominantAttributionCqpFactory = dominantAttributionCqpFactory;
        this.queryProcessingService = queryProcessingService;
        this.rankingCqpFactory = rankingCqpFactory;
    }

    @GetMapping("/temporal-aggregation")
    public ResponseEntity<?> checkTemporalAggregation(@RequestBody TemporalAggregationRequestDto requestDto){
        return new  ResponseEntity<>(
                queryProcessingService.process(temporalAggregationCqpFactory.fromDto(requestDto)),
                HttpStatus.OK
        );
    }
    @GetMapping("/temporal-count")
    public ResponseEntity<?> checkTemporalCount(@RequestBody TemporalCountDto requestDto){
        return new  ResponseEntity<>(
                queryProcessingService.process(temporalCountCqpFactory.fromDto(requestDto)),
                HttpStatus.OK
        );
    }
    @GetMapping("/dominant-attribution")
    public ResponseEntity<?> checkDominantAttribute(@RequestBody DominantAttributeSelectionDto requestDto){
        return new  ResponseEntity<>(
                queryProcessingService.process(dominantAttributionCqpFactory.fromDto(requestDto)),
                HttpStatus.OK
        );
    }
    @GetMapping("/ranking")
    public ResponseEntity<?> checkRankingQueries(@RequestBody RankingQueryDto requestDto){
        return new  ResponseEntity<>(
                queryProcessingService.process(rankingCqpFactory.fromDto(requestDto)),
                HttpStatus.OK
        );
    }
}
