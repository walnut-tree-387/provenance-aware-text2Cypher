package com.example.text2cypher.data.cqp.factories;

import com.example.text2cypher.data.cqp.BaseConstraintExtractor;
import com.example.text2cypher.data.cqp.entities.*;
import com.example.text2cypher.data.dto.RankingQueryDto;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public class RankingCqpFactory implements CqpFactory<RankingQueryDto>{
    @Override
    public CanonicalQueryPlan fromDto(RankingQueryDto dto) {
        ClauseItem groupingKey = switch (dto.getRankDimension()) {
            case "zone" -> new ClauseItem("z.name", "key");
            case "month" -> new ClauseItem("m.code", "key");
            case "event_sub_type" -> new ClauseItem("est.name", "key");
            case "event_type" -> new ClauseItem("et.name", "key");
            default -> throw new IllegalStateException("Unexpected value: " + dto.getRankDimension());
        };

        WithClause withClause = new WithClause(
                List.of(groupingKey),
                List.of(
                        new AggregationExpression(
                                AggregationType.SUM,
                                "o.count",
                                "total"
                        )
                ),
                true
        );
        ReturnClause returnClause = new ReturnClause(
                List.of(
                        new ClauseItem("key", "value"),
                        new ClauseItem("total", "total"),
                        new ClauseItem("provenance", "provenance")
                )
        );

        return new CanonicalQueryPlan(
                QueryIntent.Ranking,
                AnswerType.LIST,
                BaseConstraintExtractor.extract(dto),
                withClause,
                new OrderByClause("total", "DESC"),
                dto.getLimit(),
                returnClause
        );
    }
}
