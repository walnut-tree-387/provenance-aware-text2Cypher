package com.example.text2cypher.data.query_types.temporal_aggregation;

import com.example.text2cypher.data.cqp.*;
import com.example.text2cypher.data.cypher.CypherBuilder;
import com.example.text2cypher.data.cypher.CypherResponse;
import com.example.text2cypher.data.cypher.CypherResponseMapper;
import com.example.text2cypher.data.dto.TemporalAggregationRequestDto;
import com.example.text2cypher.data.proto_nl.ProtoNLGenerator;
import com.example.text2cypher.neo4j.Neo4jService;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TemporalAggregationServiceImpl implements TemporalAggregationService {
    private final ProtoNLGenerator protoNLGenerator;
    private final CqpValidator cqpValidator;
    private final CypherBuilder cypherBuilder;
    private final Neo4jService neo4jService;

    public TemporalAggregationServiceImpl(ProtoNLGenerator protoNLGenerator, CqpValidator cqpValidator, CypherBuilder cypherBuilder, Neo4jService neo4jService) {
        this.protoNLGenerator = protoNLGenerator;
        this.cqpValidator = cqpValidator;
        this.cypherBuilder = cypherBuilder;
        this.neo4jService = neo4jService;
    }

    @Override
    public CypherResponse process(TemporalAggregationRequestDto requestDto) {
        CanonicalQueryPlan cqp = getQueryPlan(requestDto);
        cqpValidator.validate(cqp);
        String cypher = cypherBuilder.build(cqp);
        Record record = neo4jService.fetch(cypher);
        return CypherResponseMapper.map(record, protoNLGenerator.generate(cqp));
    }
    private CanonicalQueryPlan getQueryPlan(TemporalAggregationRequestDto requestDto) {
        AggregationType aggregationType = AggregationType.SUM;
        AnswerType answerType = AnswerType.SCALAR;
        List<Constraint> constraints = new ArrayList<>();
        if(requestDto.getZone() != null) {
            constraints.add(new Constraint(ConstraintLabel.Zone, "name", Operator.EQ, requestDto.getZone()));
        }if(requestDto.getEventType() != null) {
            constraints.add(new Constraint(ConstraintLabel.EventType, "name", Operator.EQ, requestDto.getEventType()));
        }if(requestDto.getEventSubType() != null) {
            constraints.add(new Constraint(ConstraintLabel.EventSubType, "name", Operator.EQ, requestDto.getEventSubType()));
        }if(requestDto.getMonthCode() != null) {
            constraints.add(new Constraint(ConstraintLabel.Month, "code", Operator.EQ, requestDto.getMonthCode()));
        }if(requestDto.getMonthYear() != null) {
            constraints.add(new Constraint(ConstraintLabel.Month, "year", Operator.EQ, requestDto.getMonthYear()));
            if(requestDto.getMonthQuarter() != null)
                constraints.add(new Constraint(ConstraintLabel.Month, "quarter", Operator.EQ, requestDto.getMonthQuarter()));
            if(requestDto.getMonthValue() != null)
                constraints.add(new Constraint(ConstraintLabel.Month, "month", Operator.EQ, requestDto.getMonthValue()));
        }
        return  new CanonicalQueryPlan(aggregationType, answerType, constraints);
    }
}
