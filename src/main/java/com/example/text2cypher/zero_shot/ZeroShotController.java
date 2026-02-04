package com.example.text2cypher.zero_shot;

import com.example.text2cypher.data.QueryProcessingService;
import com.example.text2cypher.data.AIS.AIS;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/zero-shot")
public class ZeroShotController {
    private final NlToAISGenerator nlToCypherGenerator;
    private final QueryProcessingService queryProcessingService;

    public ZeroShotController(NlToAISGenerator nlToCypherGenerator, QueryProcessingService queryProcessingService) {
        this.nlToCypherGenerator = nlToCypherGenerator;
        this.queryProcessingService = queryProcessingService;
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
        return new ResponseEntity<>(queryProcessingService.evaluateAIS(ais), HttpStatus.OK);
    }
}
