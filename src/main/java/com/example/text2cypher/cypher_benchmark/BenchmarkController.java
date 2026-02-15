package com.example.text2cypher.cypher_benchmark;

import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.ais_evaluation.compiler.AIStoCQPCompiler;
import com.example.text2cypher.cypher_benchmark.dto.OlapQueryDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seed")
public class BenchmarkController {
    private final BenchmarkProducer benchmarkProducer;
    private final AIStoCQPCompiler compiler;

    public BenchmarkController(BenchmarkProducer benchmarkProducer, AIStoCQPCompiler compiler) {
        this.benchmarkProducer = benchmarkProducer;
        this.compiler = compiler;
    }
    @PostMapping
    public ResponseEntity<?> seedData(@RequestBody OlapQueryDto requestDto){
        return new ResponseEntity<>(benchmarkProducer.produce(requestDto), HttpStatus.OK);
    }
    @PostMapping("/ais-cqp")
    public ResponseEntity<?> aisCqp(@RequestBody AIS ais){
        return new ResponseEntity<>(compiler.mapToCQP(ais), HttpStatus.OK);
    }
}
