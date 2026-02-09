package com.example.text2cypher.ais_evaluation.ais.intent;

import com.example.text2cypher.ais_evaluation.ais.context.AISContext;
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
