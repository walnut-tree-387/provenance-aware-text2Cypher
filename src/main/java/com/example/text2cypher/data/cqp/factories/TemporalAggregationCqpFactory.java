package com.example.text2cypher.data.cqp.factories;

import com.example.text2cypher.data.cqp.BaseConstraintExtractor;
import com.example.text2cypher.data.cqp.entities.*;
import com.example.text2cypher.data.dto.TemporalAggregationRequestDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TemporalAggregationCqpFactory implements CqpFactory<TemporalAggregationRequestDto> {

    @Override
    public CanonicalQueryPlan fromDto(TemporalAggregationRequestDto dto) {
        return new CanonicalQueryPlan(
                QueryIntent.Temporal_Aggregation,
                AnswerType.SCALAR,
                BaseConstraintExtractor.extract(dto),
                new WithClause(
                        List.of(),
                        List.of(
                                new AggregationExpression(
                                        AggregationType.COUNT_SUM,
                                        "o.count",
                                        "total", null
                                )
                        ),
                        true
                ),
                null, null,
                new ReturnClause(
                        List.of(
                                new ClauseItem("total", "answer"),
                                new ClauseItem("provenance", "provenance")
                        )
                )
        );
    }
}
