package com.example.text2cypher.cypher_benchmark;

import com.example.text2cypher.cypher_benchmark.dto.OlapQueryDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seed")
public class BenchmarkController {
    private final BenchmarkProducer benchmarkProducer;

    public BenchmarkController(BenchmarkProducer benchmarkProducer) {
        this.benchmarkProducer = benchmarkProducer;
    }
    @PostMapping
    public ResponseEntity<?> seedData(@RequestBody OlapQueryDto requestDto){
        benchmarkProducer.produce(requestDto);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
