package com.example.text2cypher.data.cqp;

import org.springframework.stereotype.Component;

@Component
public class CqpValidator {
    public void validate(CanonicalQueryPlan cqp) {
        validateStructure(cqp);
        validateAggregation(cqp);
        validateConstraints(cqp);
    }

    private void validateStructure(CanonicalQueryPlan cqp) {
        if (cqp.getAggregationType() == null)
            throw new IllegalArgumentException("Aggregation missing");
        if (cqp.getAnswerType() == null)
            throw new IllegalArgumentException("Answer type missing");
    }

    private void validateAggregation(CanonicalQueryPlan cqp) {
        if (cqp.getAggregationType() == AggregationType.SUM &&
                cqp.getAnswerType() != AnswerType.SCALAR) {
            throw new IllegalArgumentException("SUM must return scalar");
        }
    }

    private void validateConstraints(CanonicalQueryPlan cqp) {
        boolean hasMonth = cqp.getConstraints().stream()
                .anyMatch(c -> c.getLabel() == ConstraintLabel.Month);
        if (!hasMonth)
            throw new IllegalArgumentException("Temporal aggregation requires Month constraint");
    }
}
