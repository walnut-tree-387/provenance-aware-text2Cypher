package com.example.text2cypher.zero_shot;

import com.example.text2cypher.data.cqp.entities.AIS.AIS;
import com.example.text2cypher.data.cqp.entities.AIS.compilers.AIStoCQPCompiler;
import com.example.text2cypher.data.cqp.entities.OLAP_entities.CQP;
import com.example.text2cypher.data.cypher.CypherBuilder;
import com.example.text2cypher.data.cypher.OlapCypherBuilder;
import com.example.text2cypher.data.cypher.OlapCypherResponse;
import com.example.text2cypher.data.cypher.OlapCypherResponseMapper;
import com.example.text2cypher.neo4j.Neo4jService;
import org.neo4j.driver.Record;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/zero-shot")
public class ZeroShotController {
    private final NlToCypherGenerator nlToCypherGenerator;
    private final AIStoCQPCompiler cqpCompiler;
    private final Neo4jService neo4jService;
    private final OlapCypherBuilder cypherBuilder;

    public ZeroShotController(NlToCypherGenerator nlToCypherGenerator, AIStoCQPCompiler cqpCompiler, Neo4jService neo4jService, OlapCypherBuilder cypherBuilder) {
        this.nlToCypherGenerator = nlToCypherGenerator;
        this.cqpCompiler = cqpCompiler;
        this.neo4jService = neo4jService;
        this.cypherBuilder = cypherBuilder;
    }

    @GetMapping()
    public ResponseEntity<?> checkZeroShot(@RequestParam String question){
        return new ResponseEntity<>(nlToCypherGenerator.generateCypher(question), HttpStatus.OK);
    }
    @GetMapping("/ais")
    public ResponseEntity<?> generateAIS(@RequestParam String question){
        return new ResponseEntity<>(nlToCypherGenerator.generateAIS(question), HttpStatus.OK);
    }
    @GetMapping("/convert-ais")
    public ResponseEntity<?> convertAIS(@RequestBody AIS ais){
        CQP cqp = cqpCompiler.mapToCQP(ais);
        List<Record> recordList = neo4jService.fetch(cypherBuilder.build(cqp));
        OlapCypherResponse response = OlapCypherResponseMapper.map(recordList, cqp.getReturnClauses());
        System.out.println(response);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
