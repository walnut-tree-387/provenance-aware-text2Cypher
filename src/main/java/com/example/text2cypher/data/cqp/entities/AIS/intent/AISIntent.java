package com.example.text2cypher.data.cqp.entities.AIS.intent;

import com.example.text2cypher.data.cqp.entities.AIS.context.AISContext;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
@Data
@AllArgsConstructor
public class AISIntent {
    private String name;
    private AISIntentType type;
    private List<AISContext> localContext;
}
