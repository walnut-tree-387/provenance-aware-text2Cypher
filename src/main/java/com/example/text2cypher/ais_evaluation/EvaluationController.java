package com.example.text2cypher.ais_evaluation;

import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.ais_evaluation.compiler.AIStoCQPCompiler;
import com.example.text2cypher.ais_evaluation.record.EvaluationService;
import com.example.text2cypher.ais_evaluation.utils.EvaluationScheduler;
import com.example.text2cypher.cypher_utils.cqp.CQP;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
}
