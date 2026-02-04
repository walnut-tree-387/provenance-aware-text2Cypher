package com.example.text2cypher.data.cqp;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Projection {
    private List<String> fields;
}
