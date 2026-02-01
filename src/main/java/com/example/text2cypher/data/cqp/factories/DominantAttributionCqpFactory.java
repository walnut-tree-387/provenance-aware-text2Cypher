package com.example.text2cypher.data.cqp.factories;

import com.example.text2cypher.data.cqp.BaseConstraintExtractor;
import com.example.text2cypher.data.cqp.entities.*;
import com.example.text2cypher.data.dto.DominantAttributeSelectionDto;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DominantAttributionCqpFactory implements CqpFactory<DominantAttributeSelectionDto>{
    @Override
    public CanonicalQueryPlan fromDto(DominantAttributeSelectionDto dto) {
        ClauseItem groupKey = switch (dto.getDominantDimension()) {
            case "zone" -> new ClauseItem("z.name", "key");
            case "subtype" -> new ClauseItem("est.name", "key");
            case "month" -> new ClauseItem("m.month", "key");
            case "year" -> new ClauseItem("m.year", "key");
            default -> throw new IllegalArgumentException("Unsupported dimension");
        };
        return new CanonicalQueryPlan(
                QueryIntent.Dominant_Attribution,
                AnswerType.SINGLE_ENTITY,
                BaseConstraintExtractor.extract(dto),
                new WithClause(
                        List.of(groupKey),
                        List.of(new AggregationExpression(
                                AggregationType.COUNT_SUM,
                                "o.count",
                                "total", null
                        )),
                        true
                ),
                new OrderByClause("total", "DESC"), 1L,
                new ReturnClause(
                        List.of(
                                new ClauseItem("key", "answer"),
                                new ClauseItem("provenance", "provenance")
                        )
                )
        );
    }
}
