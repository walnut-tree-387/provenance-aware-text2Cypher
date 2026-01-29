package com.example.text2cypher.data.cqp.entities.AIS.fact;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AISFact {
    private AISFactNode node;
    private String field;
}
