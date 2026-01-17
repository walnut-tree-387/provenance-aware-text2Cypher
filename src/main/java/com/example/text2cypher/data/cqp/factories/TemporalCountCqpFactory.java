package com.example.text2cypher.data.cqp.factories;

import com.example.text2cypher.data.cqp.BaseConstraintExtractor;
import com.example.text2cypher.data.cqp.entities.*;
import com.example.text2cypher.data.dto.TemporalCountDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TemporalCountCqpFactory implements CqpFactory<TemporalCountDto> {
    @Override
    public CanonicalQueryPlan fromDto(TemporalCountDto dto) {
        return new CanonicalQueryPlan(
                QueryIntent.Temporal_Count,
                AnswerType.SCALAR,
                BaseConstraintExtractor.extract(dto),
                new WithClause(
                        List.of(
                                new ClauseItem("o.count", "count")
                        ),
                        List.of(),
                        true
                ), null, null,
                new ReturnClause(
                        List.of(
                                new ClauseItem("count", "answer"),
                                new ClauseItem("provenance", "provenance")
                        )
                )
        );
    }
}
