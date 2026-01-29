package com.example.text2cypher.data;

import com.example.text2cypher.data.cqp.factories.*;
import com.example.text2cypher.data.cypher.OlapCypherBuilder;
import com.example.text2cypher.data.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/query-types")
public class QueryTypeController {
    private final TemporalCountCqpFactory temporalCountCqpFactory;
    private final TemporalAggregationCqpFactory temporalAggregationCqpFactory;
    private final DominantAttributionCqpFactory dominantAttributionCqpFactory;
    private final QueryProcessingService queryProcessingService;
    private final RankingCqpFactory rankingCqpFactory;
    private final RatioCqpFactory ratioCqpFactory;
    private final OlapCqpFactory cqpFactory;
    private final OlapCypherBuilder cypherBuilder;

    public QueryTypeController(TemporalCountCqpFactory temporalCountCqpFactory,
                               TemporalAggregationCqpFactory temporalAggregationCqpFactory,
                               DominantAttributionCqpFactory dominantAttributionCqpFactory, QueryProcessingService queryProcessingService, RankingCqpFactory rankingCqpFactory, RatioCqpFactory ratioCqpFactory, OlapCqpFactory cqpFactory, OlapCypherBuilder cypherBuilder) {
        this.temporalCountCqpFactory = temporalCountCqpFactory;
        this.temporalAggregationCqpFactory = temporalAggregationCqpFactory;
        this.dominantAttributionCqpFactory = dominantAttributionCqpFactory;
        this.queryProcessingService = queryProcessingService;
        this.rankingCqpFactory = rankingCqpFactory;
        this.ratioCqpFactory = ratioCqpFactory;
        this.cqpFactory = cqpFactory;
        this.cypherBuilder = cypherBuilder;
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
    @GetMapping("/ratio")
    public ResponseEntity<?> checkRatioQueries(@RequestBody RatioQueryDto requestDto){
        return new ResponseEntity<>(
                queryProcessingService.process(ratioCqpFactory.fromDto(requestDto)), HttpStatus.OK
        );
    }
    @GetMapping("/olap")
    public ResponseEntity<?> getOlapAlgebra(@RequestBody OlapQueryDto requestDto){
        return new ResponseEntity<>(Map.of("cypher", cypherBuilder.build((cqpFactory.fromDto(requestDto)))), HttpStatus.OK);
    }
}
