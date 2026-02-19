package com.example.text2cypher.ais_evaluation.utils;

import com.example.text2cypher.ais_evaluation.utils.filter_utils.SemanticFilterNormalizer;
import com.example.text2cypher.cypher_utils.cqp.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.text2cypher.ais_evaluation.utils.filter_utils.SemanticFilterNormalizer.subtypeToTypeMap;
import static com.example.text2cypher.ais_evaluation.utils.filter_utils.SemanticFilterNormalizer.zoneToDivisionMap;
import static com.example.text2cypher.graph_generation.graph.nodes.EventSubType.SEVERITY_MAP;

@Component
public class ExactMatchAccuracy {
    private static final SemanticFilterNormalizer normalizer = new SemanticFilterNormalizer();
    private static final LinkedHashMap<String, String> aliasMap = new LinkedHashMap<>();

    public static Long getCorrectScore(CQP goldCQP, CQP predicted){
        if(predicted == null) return 0L;
        Long correct = 0L;
        correct += getGroupKeyScore(goldCQP, predicted);
        correct += getMeasureScore(goldCQP, predicted);
        correct += getPostAggregationScore(goldCQP.getPostAggregations(), predicted.getPostAggregations());
        correct += getOrderScore(goldCQP.getOrderClauses(), predicted.getOrderClauses());
        correct += goldCQP.getLimit() != null && predicted.getLimit() != null && Objects.equals(goldCQP.getLimit(), predicted.getLimit()) ? 1 : 0;
        correct += goldCQP.getOffset() != null && predicted.getOffset() != null &&  Objects.equals(goldCQP.getOffset(), predicted.getOffset()) ? 1 : 0;
        correct += getProjectionScore(goldCQP.getReturnClauses(), predicted.getReturnClauses());
        aliasMap.clear();
        return correct;
    }
    private static boolean filtersEqual(List<Filter> gold, List<Filter> predicted) {
        Map<Dimension, Set<String>> goldDimensions = normalizer.normalize(gold);
        Map<Dimension, Set<String>> predictedDimensions = normalizer.normalize(predicted);
        return (goldDimensions.get(Dimension.EVENT_SUBTYPE).equals(predictedDimensions.get(Dimension.EVENT_SUBTYPE))
                || (goldDimensions.get(Dimension.EVENT_SUBTYPE).isEmpty() && predictedDimensions.get(Dimension.EVENT_SUBTYPE).equals(allEventSubTypes())))
                && (goldDimensions.get(Dimension.MONTH_CODE).equals(predictedDimensions.get(Dimension.MONTH_CODE))
                || (goldDimensions.get(Dimension.MONTH_CODE).isEmpty() && predictedDimensions.get(Dimension.MONTH_CODE).equals(allMonthCodes())))
                && (goldDimensions.get(Dimension.ZONE_NAME).equals(predictedDimensions.get(Dimension.ZONE_NAME))
                || (goldDimensions.get(Dimension.ZONE_NAME).isEmpty() && predictedDimensions.get(Dimension.ZONE_NAME).equals(allZoneNames())));
    }
    private static Long getMeasureScore(CQP goldCQP, CQP predictedCQP){
        long correctlyPredicted = 0L;
        List<Measure> remainingPredicted = new ArrayList<>(predictedCQP.getMeasures());
        List<Filter> mergedGoldFilters = goldCQP.getFilters();
        List<Filter> mergedPredictedFilters = predictedCQP.getFilters();
        for (Measure goldMeasure : goldCQP.getMeasures()) {
            mergedGoldFilters.addAll(goldMeasure.getFilters());
            Optional<Measure> match = Optional.empty();
            for (Measure predMeasure : remainingPredicted) {
                if(!predMeasure.getAggregationType().equals(goldMeasure.getAggregationType())) continue;
                mergedPredictedFilters.addAll(predMeasure.getFilters());
                if(filtersEqual(mergedGoldFilters, mergedPredictedFilters)){
                    match = Optional.of(predMeasure);
                }
                mergedPredictedFilters.removeAll(predMeasure.getFilters());
            }
            if (match.isPresent()) {
                correctlyPredicted++;
                aliasMap.put(goldMeasure.getAlias(), match.get().getAlias());
                remainingPredicted.remove(match.get());
            }
            mergedGoldFilters.removeAll(goldMeasure.getFilters());
        }
        return correctlyPredicted;
    }
    private static Long getGroupKeyScore(CQP gold, CQP predicted){
        long correctlyPredicted = 0L;
        List<GroupKey> remainingPredicted = new ArrayList<>(predicted.getGroupBy());
        for (GroupKey goldKey : gold.getGroupBy()) {
            Optional<GroupKey> match = remainingPredicted.stream()
                    .filter(pk -> pk.getDimension().equals(goldKey.getDimension()) || isMonthEquality(goldKey.getDimension(), pk.getDimension()))
                    .findFirst();
            if (match.isPresent()) {
                correctlyPredicted++;
                aliasMap.put(goldKey.getAlias(), match.get().getAlias());
                remainingPredicted.remove(match.get());
            }
        }
        return correctlyPredicted;
    }

    private static boolean postAggregationExactlyMatch(PostAggregation predicted, PostAggregation gold) {
        // If both post-aggregation type and operands match with respective order then it's a match
        if(predicted.getType() != gold.getType()) return false;
        return predicted.getOparandList().getFirst().equals(aliasMap.get(gold.getOparandList().getFirst()))
                && (predicted.getOparandList().getLast().equals(aliasMap.get(gold.getOparandList().getLast()))
                || predicted.getOparandList().getLast().equals(gold.getOparandList().getLast()));
    }
    private static boolean orderExactMatch(OrderSpec predicted, OrderSpec gold) {
        if(predicted == null) return false;
        if(!predicted.getDirection().equals(gold.getDirection()) || !predicted.getOrderType().equals(gold.getOrderType()))return false;
        return predicted.getField().equals(aliasMap.get(gold.getField()));
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
    public static Long getPredictedScore(CQP predicted){
        if(predicted == null) return 0L;
        long predictedScore = predicted.getReturnClauses().size() + predicted.getOrderClauses().size() + predicted.getMeasures().size() +
                predicted.getGroupBy().size() + predicted.getPostAggregations().size();
        if(predicted.getLimit() != null) predictedScore++;
        if(predicted.getOffset() != null) predictedScore++;
        return predictedScore;
    }
    public static Set<String> allEventSubTypes(){
        return subtypeToTypeMap.keySet();
    }
    public static Set<String> allEventTypes(){
        return new HashSet<>(subtypeToTypeMap.values());
    }
    public static Set<String> allSeverities() {
        return SEVERITY_MAP.values().stream()
                .map(String::valueOf)
                .collect(Collectors.toSet());
    }
    public static Set<String> allZoneNames(){
        return zoneToDivisionMap.keySet();
    }
    public static Set<String> allZoneDivisions(){
        return new HashSet<>(zoneToDivisionMap.values());
    }
    public static Set<String> allMonthCodes() {
        Set<Integer> years = new HashSet<>(Set.of(2019, 2020, 2021, 2022, 2023, 2024, 2025));
        return years.stream()
                .flatMap(year -> IntStream.rangeClosed(1, 12)
                        .mapToObj(month -> String.format("%d-%02d", year, month)))
                .collect(Collectors.toSet());
    }
    public static Set<String> allYears(){
        return new HashSet<>(Set.of("2019", "2020", "2021", "2022", "2023", "2024", "2025"));
    }
    public static Set<String> allMonths(){
        return new HashSet<>(Set.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"));
    }
    public static Set<String> allQuarters(){
        return new HashSet<>(Set.of("1", "2", "3", "4"));
    }
    public static boolean isMonthEquality(Dimension dim1, Dimension dim2){
        return (dim1.equals(Dimension.MONTH) && dim2.equals(Dimension.MONTH_CODE)) || (dim2.equals(Dimension.MONTH) && dim1.equals(Dimension.MONTH_CODE));
    }
}
