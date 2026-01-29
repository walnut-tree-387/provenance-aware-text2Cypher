package com.example.text2cypher.data.cqp.entities.OLAP_entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Projection {
    private List<String> fields;
}
