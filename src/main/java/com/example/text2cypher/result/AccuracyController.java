package com.example.text2cypher.result;

import com.example.text2cypher.ais_evaluation.utils.EvaluationScheduler;
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
    private final EvaluationScheduler evaluationScheduler;

    public AccuracyController(AccuracyCalculator accuracyCalculator, EvaluationScheduler evaluationScheduler) {
        this.accuracyCalculator = accuracyCalculator;
        this.evaluationScheduler = evaluationScheduler;
    }

    @GetMapping("/ais")
    public ResponseEntity<?> getAisAccuracy(@RequestParam QueryType queryType) {
        return new ResponseEntity<>(accuracyCalculator.calculateAisAccuracy(queryType), HttpStatus.OK);
    }
    @GetMapping("/cypher")
    public ResponseEntity<?> getCypherAccuracy(@RequestParam QueryType queryType) {
        evaluationScheduler.generateFineTuneData();
        return new ResponseEntity<>(accuracyCalculator.calculateCypherAccuracy(queryType), HttpStatus.OK);
    }
    @GetMapping("/compare-accuracy")
    public ResponseEntity<?> compareAccuracy() {
        return new ResponseEntity<>(accuracyCalculator.compareAccuracy(), HttpStatus.OK);
    }
}
