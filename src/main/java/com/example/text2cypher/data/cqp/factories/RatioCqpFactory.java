package com.example.text2cypher.data.cqp.factories;

import com.example.text2cypher.data.cqp.BaseConstraintExtractor;
import com.example.text2cypher.data.cqp.entities.*;
import com.example.text2cypher.data.dto.RatioQueryDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RatioCqpFactory implements CqpFactory<RatioQueryDto>{
    @Override
    public CanonicalQueryPlan fromDto(RatioQueryDto dto) {
        AggregationExpression denominator =  new AggregationExpression(AggregationType.SUM, "o.count", "denominator", null);
        AggregationExpression numerator = new AggregationExpression(AggregationType.SUM, "o.count", "numerator", dto.getConstraint());
        DerivedExpression ratioExpr = new DerivedExpression("(numerator * 1.0 / denominator) * 100", "percentage");
        WithClause withClause = new WithClause(
                List.of(), List.of(numerator, denominator, ratioExpr),true
        );
        return new CanonicalQueryPlan(
                QueryIntent.Ratio,
                AnswerType.PERCENTAGE,
                BaseConstraintExtractor.extract(dto),
                withClause,
                null, null, new ReturnClause(
                List.of(new ClauseItem("percentage", "answer"), new ClauseItem("provenance", "provenance")))
        );
    }
}
