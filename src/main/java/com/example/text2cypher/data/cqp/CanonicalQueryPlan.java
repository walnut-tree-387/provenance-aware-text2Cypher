package com.example.text2cypher.data.cqp;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CanonicalQueryPlan {
    private AggregationType aggregationType;
    private AnswerType answerType;
    private List<Constraint> constraints;
}
