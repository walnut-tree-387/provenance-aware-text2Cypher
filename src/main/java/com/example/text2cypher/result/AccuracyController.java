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

    public AccuracyController(AccuracyCalculator accuracyCalculator) {
        this.accuracyCalculator = accuracyCalculator;
    }

    @GetMapping("/ais")
    public ResponseEntity<?> getAisAccuracy() {
        return new ResponseEntity<>(accuracyCalculator.calculateMeanAisPrediction(), HttpStatus.OK);
    }
    @GetMapping("/ais-queryType")
    public ResponseEntity<?> getAisAccuracy(@RequestParam("queryType") QueryType queryType, @RequestParam("stage") Long stage) {
        return new ResponseEntity<>(accuracyCalculator.calculateZeroShotAIS2CypherQueryTypeAccuracyPrediction(queryType, stage), HttpStatus.OK);
    }
    @GetMapping("/cypher")
    public ResponseEntity<?> getCypherAccuracy(@RequestParam QueryType queryType) {
        return new ResponseEntity<>(accuracyCalculator.calculateCypherAccuracy(queryType), HttpStatus.OK);
    }
    @GetMapping("/few-shot/cypher")
    public ResponseEntity<?> getFewShotCypherAccuracy(@RequestParam QueryType queryType) {
        return new ResponseEntity<>(accuracyCalculator.calculateFewShotCypherAccuracy(queryType), HttpStatus.OK);
    }
    @GetMapping("/compare-accuracy")
    public ResponseEntity<?> compareAccuracy() {
        return new ResponseEntity<>(accuracyCalculator.summarizeAccuracy(), HttpStatus.OK);
    }
}
