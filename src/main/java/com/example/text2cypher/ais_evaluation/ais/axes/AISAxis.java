package com.example.text2cypher.ais_evaluation.ais.axes;

import com.example.text2cypher.ais_evaluation.ais.context.AISDimension;
import lombok.Data;

@Data
public class AISAxis {
    private AISDimension dimension;
    private String name;
}
