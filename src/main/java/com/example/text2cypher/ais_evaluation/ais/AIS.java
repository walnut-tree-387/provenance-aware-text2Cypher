package com.example.text2cypher.ais_evaluation.ais;

import com.example.text2cypher.ais_evaluation.ais.axes.AISAxis;
import com.example.text2cypher.ais_evaluation.ais.context.AISContext;
import com.example.text2cypher.ais_evaluation.ais.derived_intent.AISDerivedIntent;
import com.example.text2cypher.ais_evaluation.ais.fact.AISFact;
import com.example.text2cypher.ais_evaluation.ais.intent.AISIntent;
import com.example.text2cypher.ais_evaluation.ais.order.AISOrderIntent;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AIS {
    AISFact fact = new AISFact("Observation", "count");
    List<AISContext> context = new ArrayList<>();
    List<AISAxis> axes = new ArrayList<>();
    List<AISIntent> intents = new ArrayList<>();
    List<AISDerivedIntent> derivedIntents = new ArrayList<>();
    List<AISOrderIntent> orderIntents = new ArrayList<>();
    Integer limit = null;
    Integer offset = null;
    List<String> projection = new ArrayList<>();
}
