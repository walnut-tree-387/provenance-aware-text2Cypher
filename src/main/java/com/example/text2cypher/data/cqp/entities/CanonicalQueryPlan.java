package com.example.text2cypher.data.cqp.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CanonicalQueryPlan {
    private QueryIntent queryIntent;
    private AnswerType answerType;
    private List<Constraint> constraints;
    private WithClause withClause;
    private OrderByClause orderByClause;
    private Long limit;
    private ReturnClause returnClause;
}