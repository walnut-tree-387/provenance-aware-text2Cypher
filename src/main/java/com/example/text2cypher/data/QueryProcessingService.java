package com.example.text2cypher.data;

import com.example.text2cypher.data.AIS.AIS;
import com.example.text2cypher.data.AIS.compilers.AIStoCQPCompiler;
import com.example.text2cypher.data.cqp.CQP;
import com.example.text2cypher.data.factories.OlapCqpFactory;
import com.example.text2cypher.data.cypher.OlapCypherBuilder;
import com.example.text2cypher.data.cypher.OlapCypherResponse;
import com.example.text2cypher.data.cypher.OlapCypherResponseMapper;
import com.example.text2cypher.data.dto.OlapQueryDto;
import com.example.text2cypher.data.proto_nl.ProtoNLGenerator;
import com.example.text2cypher.neo4j.Neo4jService;
import com.example.text2cypher.paraphrasing.service.ProtoNLParaphraser;
import com.example.text2cypher.zero_shot.NlToAISGenerator;
import org.springframework.stereotype.Service;

import org.neo4j.driver.Record;
import java.util.List;

@Service
public class QueryProcessingService {
    private final OlapCypherBuilder cypherBuilder;
    private final Neo4jService neo4jService;
    private final AIStoCQPCompiler cqpCompiler;
    private final OlapCqpFactory cqpFactory;
    private final ProtoNLGenerator protoNLGenerator;
    private final ProtoNLParaphraser protoNLParaphraser;
    private final NlToAISGenerator nlToAISGenerator;

    public QueryProcessingService(OlapCypherBuilder cypherBuilder, Neo4jService neo4jService, AIStoCQPCompiler compiler, OlapCqpFactory cqpFactory, ProtoNLGenerator protoNLGenerator, ProtoNLParaphraser protoNLParaphraser, NlToAISGenerator nlToAISGenerator) {
        this.cypherBuilder = cypherBuilder;
        this.neo4jService = neo4jService;
        this.cqpCompiler = compiler;
        this.cqpFactory = cqpFactory;
        this.protoNLGenerator = protoNLGenerator;
        this.protoNLParaphraser = protoNLParaphraser;
        this.nlToAISGenerator = nlToAISGenerator;
    }


    public OlapCypherResponse evaluateAIS(AIS ais) {
        CQP cqp = cqpCompiler.mapToCQP(ais);
        List<Record> recordList = neo4jService.fetch(cypherBuilder.build(cqp));
        return OlapCypherResponseMapper.map(recordList, cqp.getReturnClauses());
    }
    public void buildInferencePipeline(OlapQueryDto requestDto){
        CQP goldCqp = cqpFactory.fromDto(requestDto);
        String protoNL = protoNLGenerator.generate(goldCqp);
        String goldCypher = cypherBuilder.build(goldCqp);
        OlapCypherResponse goldResponse = OlapCypherResponseMapper.map(neo4jService.fetch(goldCypher), goldCqp.getReturnClauses());
        List<String> nLQuestions = protoNLParaphraser.paraphrase(protoNL);
        for(String question : nLQuestions){
            AIS ais = (AIS) nlToAISGenerator.generateAIS(question);
            CQP testCQP = cqpCompiler.mapToCQP(ais);
//            if(testCQP.equals(goldCqp))    Exact Match Pass
            String testCypher = cypherBuilder.build(testCQP);
            OlapCypherResponse testResponse = OlapCypherResponseMapper.map(neo4jService.fetch(testCypher), testCQP.getReturnClauses());
//            if(goldResponse.equals(testResponse)) Execution Pass And Provenance Pass
        }
    }
}
