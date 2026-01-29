package com.example.text2cypher.zero_shot;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/zero-shot")
public class ZeroShotController {
    private final NlToCypherGenerator nlToCypherGenerator;

    public ZeroShotController(NlToCypherGenerator nlToCypherGenerator) {
        this.nlToCypherGenerator = nlToCypherGenerator;
    }

    @GetMapping()
    public ResponseEntity<?> checkZeroShot(@RequestParam String question){
        return new ResponseEntity<>(nlToCypherGenerator.generateCypher(question), HttpStatus.OK);
    }
    @GetMapping("/ais")
    public ResponseEntity<?> generateAIS(@RequestParam String question){
        return new ResponseEntity<>(nlToCypherGenerator.generateAIS(question), HttpStatus.OK);
    }
}
