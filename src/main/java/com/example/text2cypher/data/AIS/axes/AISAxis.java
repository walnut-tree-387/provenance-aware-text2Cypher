package com.example.text2cypher.data.AIS.axes;

import com.example.text2cypher.data.AIS.context.AISDimension;
import lombok.Data;

@Data
public class AISAxis {
    private AISDimension dimension;
    private String name;
}
