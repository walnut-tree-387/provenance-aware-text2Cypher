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
    public ResponseEntity<?> getICSCapacity() {
        return new ResponseEntity<>(accuracyCalculator.calculateModelICSCapacity(), HttpStatus.OK);
    }
    @GetMapping("/ais-queryType")
    public ResponseEntity<?> getQueryTypeAisAccuracy(@RequestParam("queryType") QueryType queryType) {
        return new ResponseEntity<>(accuracyCalculator.calculateQueryTypeAisAccuracy(queryType), HttpStatus.OK);
    }
//    @GetMapping("/cypher")
//    public ResponseEntity<?> getCypherAccuracy(@RequestParam QueryType queryType) {
//        return new ResponseEntity<>(accuracyCalculator.calculateCypherAccuracy(queryType), HttpStatus.OK);
//    }
//    @GetMapping("/few-shot/cypher")
//    public ResponseEntity<?> getFewShotCypherAccuracy(@RequestParam QueryType queryType) {
//        return new ResponseEntity<>(accuracyCalculator.calculateFewShotCypherAccuracy(queryType), HttpStatus.OK);
//    }
    @GetMapping
    public ResponseEntity<?> compareAccuracy(@RequestParam("experimentName") String experimentName) {
        accuracyCalculator.calculateZeroShotAis2CypherAccuracy(experimentName);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
