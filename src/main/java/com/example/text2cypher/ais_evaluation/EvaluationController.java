package com.example.text2cypher.ais_evaluation;

import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.ais_evaluation.utils.EvaluationScheduler;
import com.example.text2cypher.ais_evaluation.utils.ThreeStageEvaluation;
import com.example.text2cypher.evaluation_split.zero_shot_ais2cypher.ZeroShotAis2CypherService;
import com.example.text2cypher.utils.LocalMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


@RestController
@RequestMapping("/api/v1/evaluate")
public class EvaluationController {
    private final EvaluationScheduler evaluationScheduler;
    private final ThreeStageEvaluation stageEvaluation;
    public EvaluationController(EvaluationScheduler evaluationScheduler, ThreeStageEvaluation stageEvaluation) {
        this.evaluationScheduler = evaluationScheduler;
        this.stageEvaluation = stageEvaluation;
    }

    @PostMapping("/{goldId}")
    public ResponseEntity<?> evaluate(@PathVariable("goldId") Long goldId){
        return new ResponseEntity<>(evaluationScheduler.evaluateGoldEntry(goldId), HttpStatus.OK);
    }
    @PostMapping("/ais/{goldId}")
    public ResponseEntity<?> evaluateSingleAIS(@RequestBody AIS ais, @PathVariable("goldId") Long goldId){
        return new ResponseEntity<>(evaluationScheduler.checkAis(ais, goldId), HttpStatus.OK);
    }
    @PostMapping("/ais/fix")
    public ResponseEntity<?> fixAIS(@RequestBody JsonNode ais){
        List<AIS> aisList = LocalMapper.readListOneByOne(ais, AIS.class);
        stageEvaluation.reProcessFailedAIS(aisList);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    @PutMapping("/null-predicted-cypher")
    public ResponseEntity<?> updateNullPredictedZeroShotDirectCypher(@RequestBody List<String> cyphers){
        evaluationScheduler.updateNullPredictedZeroShotCypherFromInput(cyphers);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
