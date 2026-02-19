package com.example.text2cypher.ais_evaluation;

import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.ais_evaluation.utils.EvaluationScheduler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;


@RestController
@RequestMapping("/api/v1/evaluate")
public class EvaluationController {
    private final EvaluationScheduler evaluationScheduler;
    public EvaluationController(EvaluationScheduler evaluationScheduler) {
        this.evaluationScheduler = evaluationScheduler;
    }

    @PostMapping("/{goldId}")
    public ResponseEntity<?> evaluate(@PathVariable("goldId") Long goldId){
        return new ResponseEntity<>(evaluationScheduler.evaluateGoldEntry(goldId), HttpStatus.OK);
    }
    @PostMapping("/ais/{goldId}")
    public ResponseEntity<?> evaluateSingleAIS(@RequestBody AIS ais, @PathVariable("goldId") Long goldId){
        return new ResponseEntity<>(evaluationScheduler.checkAis(ais, goldId), HttpStatus.OK);
    }
    @PostMapping("/retain")
    public ResponseEntity<?> retain(@RequestBody Set<String> set){
        Set<String> retainedSet = Set.of("crime", "murder");
        set.retainAll(retainedSet);
        return new ResponseEntity<>(set, HttpStatus.OK);
    }
}
