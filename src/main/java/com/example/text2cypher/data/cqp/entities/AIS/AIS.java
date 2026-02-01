package com.example.text2cypher.data.cqp.entities.AIS;

import com.example.text2cypher.data.cqp.entities.AIS.axes.AISAxis;
import com.example.text2cypher.data.cqp.entities.AIS.context.AISContext;
import com.example.text2cypher.data.cqp.entities.AIS.derived_intent.AISDerivedIntent;
import com.example.text2cypher.data.cqp.entities.AIS.fact.AISFact;
import com.example.text2cypher.data.cqp.entities.AIS.intent.AISIntent;
import com.example.text2cypher.data.cqp.entities.AIS.order.AISOrderIntent;
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
