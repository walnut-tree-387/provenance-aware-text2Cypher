package com.example.text2cypher.data.AIS.order;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AISOrderIntent {
    private String by;
    private AISOrderDirection direction;
}
