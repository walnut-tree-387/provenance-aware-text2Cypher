package com.example.text2cypher.ais_evaluation.utils;

import com.example.text2cypher.cypher_utils.cqp.*;
import com.example.text2cypher.utils.LocalMapper;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ExactMatchAccuracy {
    private static final LinkedHashMap<String, String> aliasMap = new LinkedHashMap<>();
    public static Long getPredictedScore(CQP predicted){
        convertMonthCodeFilter(predicted.getFilters());
        long predictedScore = predicted.getReturnClauses().size() + predicted.getOrderClauses().size() + predicted.getMeasures().size() +
                predicted.getFilters().size() + predicted.getGroupBy().size() + predicted.getPostAggregations().size();
        if(predicted.getLimit() != null) predictedScore++;
        if(predicted.getOffset() != null) predictedScore++;
        return predictedScore;
    }
    public static Long getCorrectScore(CQP goldCQP, CQP predicted){
        Long correct = 0L;
        correct += getFilterMatchScore(goldCQP.getFilters(), predicted.getFilters());
        correct += getGroupKeyScore(goldCQP.getGroupBy(), predicted.getGroupBy());
        correct += getMeasureScore(goldCQP.getMeasures(), predicted.getMeasures());
        correct += getPostAggregationScore(goldCQP.getPostAggregations(), predicted.getPostAggregations());
        correct += getOrderScore(goldCQP.getOrderClauses(), predicted.getOrderClauses());
        correct += goldCQP.getLimit() != null && predicted.getLimit() != null && Objects.equals(goldCQP.getLimit(), predicted.getLimit()) ? 1 : 0;
        correct += goldCQP.getOffset() != null && predicted.getOffset() != null &&  Objects.equals(goldCQP.getOffset(), predicted.getOffset()) ? 1 : 0;
        correct += getProjectionScore(goldCQP.getReturnClauses(), predicted.getReturnClauses());
        return correct;
    }
    private static Long getProjectionScore(List<String> gold, List<String> predicted){
        long score = 0L;
        List<String> remainingPredicted = new ArrayList<>(predicted);
        for(String goldKey : gold){
            // if prediction projection list has correspondence of gold projection alias then score increase.
            boolean isPresent = remainingPredicted.stream().anyMatch(p -> p.equals(aliasMap.get(goldKey)));
            if(isPresent)score++;
        }
        return score;
    }
    private static Long getOrderScore(List<OrderSpec> gold, List<OrderSpec> predicted){
        long correctlyPredicted = 0;
        List<OrderSpec> remainingPredicted = new ArrayList<>(predicted);
        for(OrderSpec goldOrder : gold){
            Optional<OrderSpec> match = remainingPredicted.stream()
                    .filter(s -> orderExactMatch(s, goldOrder))
                    .findFirst();
            if (match.isPresent()) {
                correctlyPredicted++;
                remainingPredicted.remove(match.get());
            }
        }
        return correctlyPredicted;
    }
    private static Long getPostAggregationScore(List<PostAggregation> gold, List<PostAggregation> predicted){
        long correctlyPredicted = 0;
        List<PostAggregation> remainingPredicted = new ArrayList<>(predicted);
        for (PostAggregation postAggregation : gold){
            Optional<PostAggregation> match = remainingPredicted.stream()
                    .filter(m -> postAggregationExactlyMatch(m, postAggregation))
                    .findFirst();
            if (match.isPresent()) {
                correctlyPredicted++;
                aliasMap.put(postAggregation.getName(), match.get().getName());
                remainingPredicted.remove(match.get());
            }
        }
        return correctlyPredicted;
    }
    private static Long getMeasureScore(List<Measure> gold, List<Measure> predicted){
        long correctlyPredicted = 0L;
        List<Measure> remainingPredicted = new ArrayList<>(predicted);
        for (Measure measure : gold) {
            Optional<Measure> match = remainingPredicted.stream()
                    .filter(m -> measureExactlyMatch(m, measure))
                    .findFirst();
            if (match.isPresent()) {
                correctlyPredicted++;
                aliasMap.put(measure.getAlias(), match.get().getAlias());
                remainingPredicted.remove(match.get());
            }
        }
        return correctlyPredicted;
    }
    private static Long getGroupKeyScore(List<GroupKey> gold, List<GroupKey> predicted){
        long correctlyPredicted = 0L;
        List<GroupKey> remainingPredicted = new ArrayList<>(predicted);
        for (GroupKey goldKey : gold) {
            Optional<GroupKey> match = remainingPredicted.stream()
                    .filter(gk -> gk.getDimension().equals(goldKey.getDimension()))
                    .findFirst();
            if (match.isPresent()) {
                correctlyPredicted++;
                aliasMap.put(goldKey.getAlias(), match.get().getAlias());
                remainingPredicted.remove(match.get());
            }
        }
        return correctlyPredicted;
    }
    private static Long getFilterMatchScore(List<Filter> gold, List<Filter> predicted){
        if(gold == null || predicted == null || gold.isEmpty() || predicted.isEmpty()) return 0L;
        convertMonthCodeFilter(gold);
        long correctlyPredicted = 0L;
        List<Filter> remainingPredicted = new ArrayList<>(predicted);
        for (Filter goldFilter : gold) {
            Optional<Filter> match = remainingPredicted.stream()
                    .filter(p -> filtersExactlyMatch(goldFilter, p))
                    .findFirst();
            if (match.isPresent()) {
                correctlyPredicted++;
                remainingPredicted.remove(match.get());
            }
        }
        return correctlyPredicted;
    }
    private static boolean filtersExactlyMatch(Filter gold, Filter predicted) {
        return gold.getDimension() == predicted.getDimension()
                && gold.getOperator() == predicted.getOperator()
                && Objects.equals(gold.getValue(), predicted.getValue());
    }
    private static boolean measureExactlyMatch(Measure predicted, Measure gold) {
        if(!predicted.getAggregationType().equals(gold.getAggregationType())) return false;
        Long localFilterScore = getFilterMatchScore(gold.getFilters(), predicted.getFilters());
        return localFilterScore == gold.getFilters().size();
    }
    private static boolean postAggregationExactlyMatch(PostAggregation predicted, PostAggregation gold) {
        // If both post-aggregation type and operands match with respective order then it's a match
        if(predicted.getType() != gold.getType()) return false;
        return predicted.getOparandList().getFirst().equals(aliasMap.get(gold.getOparandList().getFirst()))
                && (predicted.getOparandList().getLast().equals(aliasMap.get(gold.getOparandList().getLast()))
                || predicted.getOparandList().getLast().equals(gold.getOparandList().getLast()));
    }
    private static boolean orderExactMatch(OrderSpec predicted, OrderSpec gold) {
        if(!predicted.getDirection().equals(gold.getDirection()) || !predicted.getOrderType().equals(gold.getOrderType()))return false;
        return predicted.getField().equals(aliasMap.get(gold.getField()));
    }

    private static void convertMonthCodeFilter(List<Filter> filters) {
        Filter monthCodeFilter = null;
        int month = -1;
        int year = -1;
        for(Filter filter : filters) {
            if (filter.getDimension() == Dimension.MONTH_CODE
                    && filter.getOperator() == Operator.EQ) {
                String monthCode = filter.getValue().toString();
                String[] parts = monthCode.split("-");
                year = Integer.parseInt(parts[0]);
                month = Integer.parseInt(parts[1]);
                monthCodeFilter = filter;
            }
        }
        if(monthCodeFilter != null && month != -1 && year != -1){
            filters.remove(monthCodeFilter);
            filters.add(new Filter(Dimension.MONTH, Operator.EQ, month));
            filters.add(new Filter(Dimension.MONTH_YEAR, Operator.EQ, year));
        }
    }

}
