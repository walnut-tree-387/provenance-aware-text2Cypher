package com.example.text2cypher.cypher_benchmark;

import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.ais_evaluation.compiler.AIStoCQPCompiler;
import com.example.text2cypher.cypher_utils.cqp.CQP;
import com.example.text2cypher.cypher_benchmark.gold_data.GoldEntryService;
import com.example.text2cypher.cypher_benchmark.factories.OlapCqpFactory;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherBuilder;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponse;
import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponseMapper;
import com.example.text2cypher.cypher_benchmark.dto.OlapQueryDto;
import com.example.text2cypher.cypher_benchmark.proto_nl.ProtoNLGenerator;
import com.example.text2cypher.neo4j.Neo4jService;
import com.example.text2cypher.cypher_benchmark.paraphraser.ProtoNLParaphraser;
import com.example.text2cypher.utils.LocalMapper;
import org.springframework.stereotype.Service;

import org.neo4j.driver.Record;
import java.util.List;

@Service
public class BenchmarkProducer {
    private final OlapCypherBuilder cypherBuilder;
    private final Neo4jService neo4jService;
    private final AIStoCQPCompiler cqpCompiler;
    private final OlapCqpFactory cqpFactory;
    private final ProtoNLGenerator protoNLGenerator;
    private final ProtoNLParaphraser protoNLParaphraser;
    private final GoldEntryService goldEntryService;

    public BenchmarkProducer(OlapCypherBuilder cypherBuilder, Neo4jService neo4jService, AIStoCQPCompiler compiler, OlapCqpFactory cqpFactory,
                             ProtoNLGenerator protoNLGenerator, ProtoNLParaphraser protoNLParaphraser, GoldEntryService goldEntryService) {
        this.cypherBuilder = cypherBuilder;
        this.neo4jService = neo4jService;
        this.cqpCompiler = compiler;
        this.cqpFactory = cqpFactory;
        this.protoNLGenerator = protoNLGenerator;
        this.protoNLParaphraser = protoNLParaphraser;
        this.goldEntryService = goldEntryService;
    }


    public OlapCypherResponse evaluateAIS(AIS ais) {
        CQP cqp = cqpCompiler.mapToCQP(ais);
        List<Record> recordList = neo4jService.fetch(cypherBuilder.build(cqp));
        return OlapCypherResponseMapper.map(recordList, cqp.getReturnClauses());
    }
    public void produce(OlapQueryDto requestDto){
        CQP goldCqp = cqpFactory.fromDto(requestDto);
        String protoNL = protoNLGenerator.generate(requestDto);
        List<String> paraphraseList = protoNLParaphraser.paraphrase(protoNL);
        String goldCypher = cypherBuilder.build(goldCqp);
        OlapCypherResponse goldResponse = OlapCypherResponseMapper.map(neo4jService.fetch(goldCypher), goldCqp.getReturnClauses());
        paraphraseList.forEach(question ->
                goldEntryService.create(goldCypher, LocalMapper.write(goldResponse.nodeList()),
                        LocalMapper.write(goldResponse.results()), LocalMapper.write(goldCqp), question));
    }
}
