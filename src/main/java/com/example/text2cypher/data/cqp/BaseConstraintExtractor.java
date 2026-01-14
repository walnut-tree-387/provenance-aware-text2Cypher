package com.example.text2cypher.data.cqp;

import com.example.text2cypher.data.cqp.entities.Constraint;
import com.example.text2cypher.data.cqp.entities.ConstraintLabel;
import com.example.text2cypher.data.cqp.entities.Operator;
import com.example.text2cypher.data.dto.BaseQueryDto;

import java.util.ArrayList;
import java.util.List;

public class BaseConstraintExtractor {
    public static List<Constraint> extract(BaseQueryDto dto) {
        List<Constraint> constraints = new ArrayList<>();
        if(dto.getZone() != null) {
            constraints.add(new Constraint(ConstraintLabel.Zone, "name", Operator.EQ, dto.getZone()));
        }if(dto.getEventType() != null) {
            constraints.add(new Constraint(ConstraintLabel.EventType, "name", Operator.EQ, dto.getEventType()));
        }if(dto.getEventSubType() != null) {
            constraints.add(new Constraint(ConstraintLabel.EventSubType, "name", Operator.EQ, dto.getEventSubType()));
        }if(dto.getMonthCode() != null) {
            constraints.add(new Constraint(ConstraintLabel.Month, "code", Operator.EQ, dto.getMonthCode()));
        }if(dto.getMonthYear() != null) {
            constraints.add(new Constraint(ConstraintLabel.Month, "year", Operator.EQ, dto.getMonthYear()));
            if(dto.getMonthQuarter() != null)
                constraints.add(new Constraint(ConstraintLabel.Month, "quarter", Operator.EQ, dto.getMonthQuarter()));
            if(dto.getMonthValue() != null)
                constraints.add(new Constraint(ConstraintLabel.Month, "month", Operator.EQ, dto.getMonthValue()));
        }
        return constraints;
    }
}

