package com.example.text2cypher.data.cqp.entities.AIS.order;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AISOrderIntent {
    private String by;
    private AISOrderDirection direction;
}
