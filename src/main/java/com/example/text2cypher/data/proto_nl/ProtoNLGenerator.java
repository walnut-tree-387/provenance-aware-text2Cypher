package com.example.text2cypher.data.proto_nl;
import com.example.text2cypher.data.cqp.entities.*;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ProtoNLGenerator {
    public String generate(CanonicalQueryPlan cqp) {
        boolean monthConstraintFlag = false;
        StringBuilder protNl = new StringBuilder("Return " + getProtoKeyword(cqp));
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
            if(constraint.getLabel().equals(ConstraintLabel.Zone))  {
                protNl.append(" and observed in ").append(constraint.getValue()).append(" ").append(constraint.getLabel());
            }
            if(constraint.getLabel().equals(ConstraintLabel.Month))  {
                if(!monthConstraintFlag) {
                    monthConstraintFlag = true;
                    protNl.append(" recorded ")
                            .append(translateMonthAndQuarterAndYear(constraint)).append(" ");
                }
                else protNl.append(" and ").append(translateMonthAndQuarterAndYear(constraint)).append(" ")
                        .append(constraint.getLabel());
            }
        }
        return protNl.toString();
    }
    private void sortConstraints(CanonicalQueryPlan cqp) {
        List<Constraint> constraints = cqp.getConstraints();
        constraints.sort(Comparator.comparing(Constraint::getLabel));
    }
    private String translateMonthAndQuarterAndYear(Constraint constraint) {
        Object val = constraint.getValue();
        if ("month".equals(constraint.getKey())) {
            return convertMonth(val);
        }
        else if ("quarter".equals(constraint.getKey())) {
            return switch (val) {
                case Long l when l == 1L -> "in first quarter";
                case Long l when l == 2L -> "in second quarter";
                case Long l when l == 3L -> "in third quarter";
                case Long l when l == 4L -> "in fourth quarter";
                default -> "unknown Quarter";
            };
        }
        else if ("year".equals(constraint.getKey())) {
            return "in " + constraint.getValue().toString();
        }
        else if("code".equals(constraint.getKey())) {
            String code = constraint.getValue().toString();
            String year = code.substring(0, 4);
            String month = code.substring(5, 7);
            return " in year " + year + " and " + convertMonth(Long.parseLong(month)) + " month";
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
            case Ratio ->  getRatioPhrase(cqp);
            default -> throw new IllegalArgumentException("Unknown intent: " + cqp.getQueryIntent());
        };
    }
    private String getRatioPhrase(CanonicalQueryPlan cqp) {
        List<WithExpression> expressionList = cqp.getWithClause().getExpressions();
        Constraint ratioConstraint = null;
        for(WithExpression expression : expressionList) {
            if(expression instanceof AggregationExpression && ((AggregationExpression) expression).getConstraint() != null){
                ratioConstraint = ((AggregationExpression) expression).getConstraint();
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("percentage of observation ");
        if(ratioConstraint != null  && ratioConstraint.getLabel().equals(ConstraintLabel.Month)){
            if(ratioConstraint.getKey().equals("m.code")){
                String code = ratioConstraint.getValue().toString();
                String month = code.substring(5, 7);
                String year = code.substring(0, 4);
                sb.append("recorded ").append(convertMonth(Long.parseLong(month))).append(" compared to the overall in ").append(Long.parseLong(year));
            }
            else if(ratioConstraint.getKey().equals("m.year")){
                Integer year = (Integer) ratioConstraint.getValue();
                sb.append("recorded in ").append(year).append(" compared to the other years filtered by ");
            }
            else if(ratioConstraint.getKey().equals("m.quarter")){
                ratioConstraint.setKey("quarter");
                Long quarter = Long.parseLong(ratioConstraint.getValue().toString());
                ratioConstraint.setValue(quarter);
                sb.append("recorded ").append(translateMonthAndQuarterAndYear(ratioConstraint)).append(" compared to whole year filtered by ");
            }
            else if(ratioConstraint.getKey().equals("m.month")){
                ratioConstraint.setKey("month");
                Long month = Long.parseLong(ratioConstraint.getValue().toString());
                ratioConstraint.setValue(month);
                sb.append("recorded ").append(translateMonthAndQuarterAndYear(ratioConstraint)).append(" compared to whole year filtered by ");
            }
        }
        else if(ratioConstraint != null  && ratioConstraint.getLabel().equals(ConstraintLabel.Zone)){
            sb.append("recorded in ").append(ratioConstraint.getValue().toString()).append(" compared to the overall crime in all the zone filtered by ");
        }
        else if(ratioConstraint != null  && ratioConstraint.getLabel().equals(ConstraintLabel.EventSubType)){
            sb.append(" recorded of event sub type ").append(ratioConstraint.getValue().toString()).append(" compared to all the other types of ");
        }
        return sb.toString();
    }
    private String getDominantAttributionPhrase(List<ClauseItem> groupByItems) {
        StringBuilder sb = new StringBuilder();
        groupByItems.forEach(item -> {
            String attribute = switch (item.getExpression()) {
                case "z.name" -> "name of the zone";
                case "est.name" -> "name of the event sub type";
                case "m.month" -> "name of the month";
                case "m.year" -> "the year";
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
                case "m.month" -> "top " + limit + " months";
                case "m.year" -> "top " + limit +" years";
                default -> throw new IllegalStateException("Unexpected value: " + item.getExpression());
            };
            sb.append(attribute).append(" which observed the highest number");
        });
        return sb.toString();
    }
    private String convertMonth(Object val) {
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
}
