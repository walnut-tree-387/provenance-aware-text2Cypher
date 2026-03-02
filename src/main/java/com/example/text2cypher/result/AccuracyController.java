package com.example.text2cypher.result;

import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accuracy")
public class AccuracyController {
    private final AccuracyCalculator accuracyCalculator;

    public AccuracyController(AccuracyCalculator accuracyCalculator) {
        this.accuracyCalculator = accuracyCalculator;
    }

    @GetMapping
    public ResponseEntity<?> getAccuracy(@RequestParam QueryType queryType) {
        return new ResponseEntity<>(accuracyCalculator.calculate(queryType), HttpStatus.OK);
    }
}
