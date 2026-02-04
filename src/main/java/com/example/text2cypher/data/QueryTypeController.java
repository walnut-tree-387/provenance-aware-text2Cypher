package com.example.text2cypher.data;

import com.example.text2cypher.data.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/query-types")
public class QueryTypeController {
    private final QueryProcessingService queryProcessingService;

    public QueryTypeController(QueryProcessingService queryProcessingService) {
        this.queryProcessingService = queryProcessingService;
    }
    @GetMapping("/olap")
    public ResponseEntity<?> getOlapAlgebra(@RequestBody OlapQueryDto requestDto){
        queryProcessingService.buildInferencePipeline(requestDto);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
