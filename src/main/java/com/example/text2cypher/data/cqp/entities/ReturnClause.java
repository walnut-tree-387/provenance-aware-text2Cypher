package com.example.text2cypher.data.cqp.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ReturnClause {
    private List<ClauseItem> clauseItems;
}
