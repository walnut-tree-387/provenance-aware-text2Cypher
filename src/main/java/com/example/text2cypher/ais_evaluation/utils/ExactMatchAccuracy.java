package com.example.text2cypher.ais_evaluation.utils;

import com.example.text2cypher.cypher_utils.cqp.*;
import com.example.text2cypher.utils.LocalMapper;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ExactMatchAccuracy {
    private static final LinkedHashMap<String, String> aliasMap = new LinkedHashMap<>();
    public static Long getPredictedScore(CQP predicted){
        long predictedScore = predicted.getReturnClauses().size() + predicted.getOrderClauses().size() + predicted.getMeasures().size() +
                predicted.getFilters().size() + predicted.getGroupBy().size() + predicted.getPostAggregations().size();
        if(predicted.getLimit() != null) predictedScore++;
        if(predicted.getOffset() != null) predictedScore++;
        return predictedScore;
    }
    public static Long getCorrectScore(String gold, CQP predicted){
        CQP goldCQP = LocalMapper.read(gold, CQP.class);
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
            Optional<String> match = remainingPredicted.stream()
                    .filter(s -> s.equals(aliasMap.get(goldKey))).findFirst();
            if(match.isPresent()){
                score++;
                remainingPredicted.remove(match.get());
            }
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
    private static boolean filtersExactlyMatch(Filter a, Filter b) {
        return a.getDimension() == b.getDimension()
                && a.getOperator() == b.getOperator()
                && Objects.equals(a.getValue(), b.getValue());
    }
    private static boolean measureExactlyMatch(Measure a, Measure b) {
        return a.getAggregationType() == b.getAggregationType()
                && getFilterMatchScore(a.getFilters(), b.getFilters()) == (a.getFilters().size());
    }
    private static boolean postAggregationExactlyMatch(PostAggregation a, PostAggregation b) {
        boolean matchFlag = true;
        for(String alias : b.getOperands()) {
            if(alias.equals(aliasMap.get(alias))) {
                matchFlag = false;
                break;
            }
        }
        return a.getType() == b.getType() && matchFlag;
    }
    private static boolean orderExactMatch(OrderSpec a, OrderSpec b) {
        return a.getDirection().equals(b.getDirection()) && a.getOrderType().equals(b.getOrderType())
                && b.getField().equals(aliasMap.get(a.getField()));
    }
}
