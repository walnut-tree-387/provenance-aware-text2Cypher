package com.example.text2cypher.ais_evaluation.ais;

import com.example.text2cypher.ais_evaluation.ais.axes.AISAxis;
import com.example.text2cypher.ais_evaluation.ais.context.AISContext;
import com.example.text2cypher.ais_evaluation.ais.derived_intent.AISDerivedIntent;
import com.example.text2cypher.ais_evaluation.ais.fact.AISFact;
import com.example.text2cypher.ais_evaluation.ais.intent.AISIntent;
import com.example.text2cypher.ais_evaluation.ais.order.AISOrderIntent;
import lombok.Data;

import java.util.List;

@Data
public class AIS {
    AISFact fact;
    List<AISContext> context;
    List<AISAxis> axes;
    List<AISIntent> intents;
    List<AISDerivedIntent> derivedIntents;
    List<AISOrderIntent> orderIntents;
    Integer limit;
    Integer offset;
    List<String> projection;
}
