package com.example.text2cypher.cypher_benchmark;

import com.example.text2cypher.ais_evaluation.ais.AIS;
import com.example.text2cypher.ais_evaluation.ais.compilers.AIStoCQPCompiler;
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
    private final GoldEntryService benchmarkService;

    public BenchmarkProducer(OlapCypherBuilder cypherBuilder, Neo4jService neo4jService, AIStoCQPCompiler compiler, OlapCqpFactory cqpFactory,
                             ProtoNLGenerator protoNLGenerator, ProtoNLParaphraser protoNLParaphraser, GoldEntryService benchmarkService) {
        this.cypherBuilder = cypherBuilder;
        this.neo4jService = neo4jService;
        this.cqpCompiler = compiler;
        this.cqpFactory = cqpFactory;
        this.protoNLGenerator = protoNLGenerator;
        this.protoNLParaphraser = protoNLParaphraser;
        this.benchmarkService = benchmarkService;
    }


    public OlapCypherResponse evaluateAIS(AIS ais) {
        CQP cqp = cqpCompiler.mapToCQP(ais);
        List<Record> recordList = neo4jService.fetch(cypherBuilder.build(cqp));
        return OlapCypherResponseMapper.map(recordList, cqp.getReturnClauses());
    }
    public void produce(OlapQueryDto requestDto){
        CQP goldCqp = cqpFactory.fromDto(requestDto);
        String protoNL = protoNLGenerator.generate(requestDto);
        String goldCypher = cypherBuilder.build(goldCqp);
        List<String> paraphraseList = protoNLParaphraser.paraphrase(protoNL);
        OlapCypherResponse goldResponse = OlapCypherResponseMapper.map(neo4jService.fetch(goldCypher), goldCqp.getReturnClauses());
        paraphraseList.forEach(question ->
                benchmarkService.create(goldCypher, goldResponse.nodeList().toString(), goldResponse.results().toString(), goldCqp.toString(), question));
    }
}
