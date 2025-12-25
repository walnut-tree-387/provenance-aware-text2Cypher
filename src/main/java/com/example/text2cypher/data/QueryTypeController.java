package com.example.text2cypher.data;

import com.example.text2cypher.data.dto.TemporalAggregationRequestDto;
import com.example.text2cypher.data.query_types.temporal_aggregation.TemporalAggregationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/query-types")
public class QueryTypeController {
    private final TemporalAggregationService temporalAggregationService;

    public QueryTypeController(TemporalAggregationService temporalAggregationService) {
        this.temporalAggregationService = temporalAggregationService;
    }

    @PostMapping("/temporal-aggregation")
    public ResponseEntity<?> checkTemporalAggregation(@RequestBody TemporalAggregationRequestDto requestDto){
        return new  ResponseEntity<>(temporalAggregationService.process(requestDto), HttpStatus.OK);
    }
}
