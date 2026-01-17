package com.example.text2cypher.data;

import com.example.text2cypher.data.cqp.CqpValidator;
import com.example.text2cypher.data.cqp.entities.*;
import com.example.text2cypher.data.cypher.CypherBuilder;
import com.example.text2cypher.data.cypher.CypherResponse;
import com.example.text2cypher.data.cypher.CypherResponseMapper;
import com.example.text2cypher.data.proto_nl.ProtoNLGenerator;
import com.example.text2cypher.neo4j.Neo4jService;
import com.example.text2cypher.paraphrasing.service.ProtoNLParaphraser;
import org.springframework.stereotype.Service;

import org.neo4j.driver.Record;
import java.util.List;

@Service
public class QueryProcessingService {
    private final CypherBuilder cypherBuilder;
    private final Neo4jService neo4jService;
    private final ProtoNLGenerator protoNLGenerator;
    private final CqpValidator cqpValidator;
    private final ProtoNLParaphraser protoNLParaphraser;

    public QueryProcessingService(CypherBuilder cypherBuilder, Neo4jService neo4jService, ProtoNLGenerator protoNLGenerator, CqpValidator cqpValidator, ProtoNLParaphraser protoNLParaphraser) {
        this.cypherBuilder = cypherBuilder;
        this.neo4jService = neo4jService;
        this.protoNLGenerator = protoNLGenerator;
        this.cqpValidator = cqpValidator;
        this.protoNLParaphraser = protoNLParaphraser;
    }

    public CypherResponse process(CanonicalQueryPlan cqp) {
        cqpValidator.validate(cqp);
        String cypher = cypherBuilder.build(cqp);
        List<Record> records = neo4jService.fetch(cypher);
        String protoNL = protoNLGenerator.generate(cqp);
        List<String> Nl = protoNLParaphraser.paraphrase(protoNL);
        return CypherResponseMapper.map(records, protoNL, Nl, cypher);
    }
}
