package com.example.text2cypher.ais_evaluation.ais.compilers;

import com.example.text2cypher.ais_evaluation.ais.context.AISDimension;
import com.example.text2cypher.ais_evaluation.ais.context.AISOperator;
import com.example.text2cypher.ais_evaluation.ais.derived_intent.AISDerivedType;
import com.example.text2cypher.ais_evaluation.ais.intent.AISIntentType;
import com.example.text2cypher.ais_evaluation.ais.order.AISOrderDirection;
import com.example.text2cypher.cypher_utils.cqp.*;

public class EnumCompiler {
    private EnumCompiler() {}

    public static Dimension compileDimension(AISDimension aisDimension) {
        return switch (aisDimension) {

            case MONTH -> Dimension.MONTH;
            case MONTH_YEAR -> Dimension.MONTH_YEAR;
            case MONTH_QUARTER -> Dimension.MONTH_QUARTER;
            case MONTH_CODE -> Dimension.MONTH_CODE;

            case ZONE_NAME -> Dimension.ZONE_NAME;
            case ZONE_DIVISION -> Dimension.ZONE_DIVISION;

            case EVENT_TYPE -> Dimension.EVENT_TYPE;
            case EVENT_SUBTYPE -> Dimension.EVENT_SUBTYPE;
            case EVENT_SUBTYPE_SEVERITY -> Dimension.EVENT_SUBTYPE_SEVERITY;

            default -> throw new RuntimeException(
                    "Unsupported AISDimension: " + aisDimension
            );
        };
    }
    public static Operator compileOperator(AISOperator aisOperator) {
        return switch (aisOperator) {
            case EQ -> Operator.EQ;
            case GT -> Operator.GT;
            case GTE -> Operator.GTE;
            case LT -> Operator.LT;
            case LTE -> Operator.LTE;
            case IN -> Operator.IN;
            case NOT_IN -> Operator.NOT_IN;

            default -> throw new RuntimeException(
                    "Unsupported AISOperator: " + aisOperator
            );
        };
    }
    public static AggregationType compileIntentType(AISIntentType intentType) {
        return switch (intentType) {
            case TOTAL_COUNT -> AggregationType.COUNT_SUM;
            case SEVERITY_WEIGHTED_COUNT -> AggregationType.WEIGHTED_SUM;
            default -> throw new RuntimeException(
                    "Unsupported AIS intent Type: " + intentType
            );
        };
    }
    public static PostAggregationType compileDerivedType(AISDerivedType derivedType) {
        return switch (derivedType) {
            case RATIO -> PostAggregationType.RATIO;
            case DIFFERENCE -> PostAggregationType.DIFFERENCE;
            case GT -> PostAggregationType.GT;
            case GTE -> PostAggregationType.GTE;
            case LTE -> PostAggregationType.LTE;
            case LT -> PostAggregationType.LT;
            case EQ -> PostAggregationType.EQ;
            default -> throw new RuntimeException(
                    "Unsupported AIS derived intent Type: " + derivedType
            );
        };
    }
    public static OrderDirection compileOrderDirection(AISOrderDirection direction) {
        return switch (direction) {
            case ASC -> OrderDirection.ASC;
            case DESC -> OrderDirection.DESC;
            default -> throw new RuntimeException(
                    "Unsupported AIS Order Direction Type: " + direction
            );
        };

    }



}
