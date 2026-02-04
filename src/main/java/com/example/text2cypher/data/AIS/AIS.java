package com.example.text2cypher.data.AIS;

import com.example.text2cypher.data.AIS.axes.AISAxis;
import com.example.text2cypher.data.AIS.context.AISContext;
import com.example.text2cypher.data.AIS.derived_intent.AISDerivedIntent;
import com.example.text2cypher.data.AIS.fact.AISFact;
import com.example.text2cypher.data.AIS.intent.AISIntent;
import com.example.text2cypher.data.AIS.order.AISOrderIntent;
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
