package com.example.text2cypher.data.proto_nl;
import com.example.text2cypher.data.cqp.entities.*;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ProtoNLGenerator {
    public String generate(CanonicalQueryPlan cqp) {
        boolean monthConstraintFlag = false;
        StringBuilder protNl = new StringBuilder("Return " + getProtoKeyword(cqp)+ " of ");
        sortConstraints(cqp);
        for(Constraint constraint : cqp.getConstraints()) {
            if(constraint.getLabel().equals(ConstraintLabel.EventType))  {
                protNl.append(" ").append(constraint.getLabel()).append(" ")
                        .append(constraint.getValue());
            }
            if(constraint.getLabel().equals(ConstraintLabel.EventSubType))  {
                protNl.append(" of ").append(constraint.getLabel()).append(" ")
                        .append(constraint.getValue());
            }
            if(constraint.getLabel().equals(ConstraintLabel.Month))  {
                if(!monthConstraintFlag) {
                    monthConstraintFlag = true;
                    protNl.append(" recorded in ")
                            .append(constraint.getValue());
                }
                else protNl.append(" AND ").append(translateMonthAndQuarter(constraint)).append(" ")
                        .append(constraint.getLabel());
            }
            if(constraint.getLabel().equals(ConstraintLabel.Zone))  {
                protNl.append(" Observed in ").append(constraint.getLabel()).append(" ")
                        .append(constraint.getValue());
            }
        }
        return protNl.toString();
    }
    private void sortConstraints(CanonicalQueryPlan cqp) {
        List<Constraint> constraints = cqp.getConstraints();
        constraints.sort(Comparator.comparing(Constraint::getLabel));
    }
    private String translateMonthAndQuarter(Constraint constraint) {
        Object val = constraint.getValue();
        if ("month".equals(constraint.getKey())) {
            return switch (val) {
                case Long l when l == 1L -> "in January";
                case Long l when l == 2L -> "in February";
                case Long l when l == 3L -> "in March";
                case Long l when l == 4L -> "in April";
                case Long l when l == 5L -> "in May";
                case Long l when l == 6L -> "in June";
                case Long l when l == 7L -> "in July";
                case Long l when l == 8L -> "in August";
                case Long l when l == 9L -> "in September";
                case Long l when l == 10L -> "in October";
                case Long l when l == 11L -> "in November";
                case Long l when l == 12L -> "in December";
                default -> "unknown month";
            };
        }
        else if ("quarter".equals(constraint.getKey())) {
            return switch (val) {
                case Long l when l == 1L -> "in First Quarter";
                case Long l when l == 2L -> "in Second Quarter";
                case Long l when l == 3L -> "in Third Quarter";
                case Long l when l == 4L -> "in Fourth Quarter";
                default -> "unknown Quarter";
            };
        }
        return null;
    }
    private String getProtoKeyword(CanonicalQueryPlan cqp) {
        return switch (cqp.getQueryIntent()) {
            case Temporal_Count -> "count";
            case Temporal_Aggregation -> "sum of count";
            case Temporal_Comparison -> "comparison";
            case Dominant_Attribution -> getDominantAttributionPhrase(cqp.getWithClause().getGroupByItems());
            case Ranking -> getRankingPhrase(cqp.getWithClause().getGroupByItems(), cqp.getLimit());
            case Ratio ->  "ratio";
            default -> throw new IllegalArgumentException("Unknown intent: " + cqp.getQueryIntent());
        };
    }
    private String getDominantAttributionPhrase(List<ClauseItem> groupByItems) {
        StringBuilder sb = new StringBuilder();
        groupByItems.forEach(item -> {
            String attribute = switch (item.getExpression()) {
                case "z.name" -> "name of the zone";
                case "est.name" -> "name of the event sub type";
                case "m.month" -> "name of the month";
                case "year" -> "the year";
                default -> throw new IllegalStateException("Unexpected value: " + item.getExpression());
            };
            sb.append(attribute).append(" which observed the highest number");
        });
        return sb.toString();
    }
    private String getRankingPhrase(List<ClauseItem> groupByItems, Long limit) {
        StringBuilder sb = new StringBuilder();
        groupByItems.forEach(item -> {
            String attribute = switch (item.getExpression()) {
                case "z.name" -> "top " + limit + " zones";
                case "est.name" -> "top " + limit + "event sub type";
                case "m.month" -> "top " + limit + "months";
                case "year" -> "top " + limit +" years";
                default -> throw new IllegalStateException("Unexpected value: " + item.getExpression());
            };
            sb.append(attribute).append(" which observed the highest number");
        });
        return sb.toString();
    }
}
