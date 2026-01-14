package com.example.text2cypher.data.cqp;

import com.example.text2cypher.data.cqp.entities.CanonicalQueryPlan;
import com.example.text2cypher.data.cqp.entities.ConstraintLabel;
import org.springframework.stereotype.Component;

@Component
public class CqpValidator {
    public void validate(CanonicalQueryPlan cqp) {
        validateStructure(cqp);
        validateConstraints(cqp);
    }

    private void validateStructure(CanonicalQueryPlan cqp) {
        if (cqp.getAnswerType() == null)
            throw new IllegalArgumentException("Answer type is missing");
        if (cqp.getQueryIntent() == null)
            throw new IllegalArgumentException("Query intent is missing");
    }

    private void validateConstraints(CanonicalQueryPlan cqp) {
        boolean hasMonth = cqp.getConstraints().stream()
                .anyMatch(c -> c.getLabel() == ConstraintLabel.Month);
        if (!hasMonth)
            throw new IllegalArgumentException("Temporal aggregation requires Month constraint");
    }
}
