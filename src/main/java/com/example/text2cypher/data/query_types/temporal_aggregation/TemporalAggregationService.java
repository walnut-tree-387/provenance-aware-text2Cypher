package com.example.text2cypher.data.query_types.temporal_aggregation;

import com.example.text2cypher.data.cypher.CypherResponse;
import com.example.text2cypher.data.dto.TemporalAggregationRequestDto;
import org.springframework.stereotype.Service;

@Service
public interface TemporalAggregationService {
    CypherResponse process(TemporalAggregationRequestDto requestDto);
}
