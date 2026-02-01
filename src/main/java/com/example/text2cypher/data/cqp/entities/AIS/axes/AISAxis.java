package com.example.text2cypher.data.cqp.entities.AIS.axes;

import com.example.text2cypher.data.cqp.entities.AIS.context.AISDimension;
import lombok.Data;

@Data
public class AISAxis {
    private AISDimension dimension;
    private String name;
}
