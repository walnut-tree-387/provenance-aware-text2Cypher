package com.example.text2cypher.ais_evaluation.utils.filter_utils;

import com.example.text2cypher.ais_evaluation.utils.ExactMatchAccuracy;
import com.example.text2cypher.cypher_utils.cqp.Dimension;
import com.example.text2cypher.cypher_utils.cqp.Filter;
import com.example.text2cypher.cypher_utils.cqp.Operator;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.text2cypher.graph_generation.graph.nodes.EventSubType.SEVERITY_MAP;

@Component
public class SemanticFilterNormalizer {
    public Map<Dimension, Set<String>> normalize(List<Filter> mergedFilters) {
       Map<Dimension, Set<String>> dimensionValues = new HashMap<>();
        for (Filter filter : mergedFilters) {
            Set<String> set = createDistinctSet(filter);
            dimensionValues.put(filter.getDimension(), set);
        }
        Set<String> finalMonthCodes = (dimensionValues.get(Dimension.MONTH_CODE) == null) ? new HashSet<>() : dimensionValues.get(Dimension.MONTH_CODE);
        Set<String> finalZoneNames = (dimensionValues.get(Dimension.ZONE_NAME) == null) ? new HashSet<>() : dimensionValues.get(Dimension.ZONE_NAME);
        Set<String> finalSubTypes = (dimensionValues.get(Dimension.EVENT_SUBTYPE) == null) ? new HashSet<>() : dimensionValues.get(Dimension.EVENT_SUBTYPE);
        if(dimensionValues.containsKey(Dimension.ZONE_DIVISION)){
            finalZoneNames = intersect(finalZoneNames, convertZoneDivisionToZoneNames(dimensionValues.get(Dimension.ZONE_DIVISION)));
        }
        if(dimensionValues.containsKey(Dimension.EVENT_TYPE)){
            finalSubTypes = intersect(finalSubTypes, convertEventTypeToSubType(dimensionValues.get(Dimension.EVENT_TYPE)));
        }
        if(dimensionValues.containsKey(Dimension.EVENT_SUBTYPE_SEVERITY)){
            finalSubTypes = intersect(finalSubTypes, convertSeverityToSubType(dimensionValues.get(Dimension.EVENT_SUBTYPE_SEVERITY)));
        }
        dimensionValues.remove(Dimension.EVENT_SUBTYPE_SEVERITY);
        dimensionValues.remove(Dimension.EVENT_SUBTYPE);
        dimensionValues.remove(Dimension.ZONE_DIVISION);
        dimensionValues.remove(Dimension.ZONE_NAME);
        dimensionValues.remove(Dimension.EVENT_TYPE);
        if(dimensionValues.containsKey(Dimension.MONTH_YEAR) && dimensionValues.containsKey(Dimension.MONTH)){
            finalMonthCodes = intersect(finalMonthCodes,
                    convertMonthAndYear(dimensionValues.get(Dimension.MONTH_YEAR), dimensionValues.get(Dimension.MONTH)));
        }
        else if(dimensionValues.containsKey(Dimension.MONTH_YEAR) &&  dimensionValues.containsKey(Dimension.MONTH_QUARTER)){
            finalMonthCodes = intersect(finalMonthCodes,
                    convertQuarterAndYear(dimensionValues.get(Dimension.MONTH_YEAR), dimensionValues.get(Dimension.MONTH_QUARTER)));
        }
        else if(dimensionValues.containsKey(Dimension.MONTH_YEAR)){
            finalMonthCodes = intersect(finalMonthCodes, convertYear(dimensionValues.get(Dimension.MONTH_YEAR)));
        }
        dimensionValues.clear();
        dimensionValues.put(Dimension.EVENT_SUBTYPE, finalSubTypes);
        dimensionValues.put(Dimension.ZONE_NAME, finalZoneNames);
        dimensionValues.put(Dimension.MONTH_CODE, finalMonthCodes);
        return dimensionValues;
    }
    private Set<String>   createDistinctSet(Filter filter) {
        Object value = filter.getValue();
        Operator op = filter.getOperator();
        Dimension dimension = filter.getDimension();
        if (op == Operator.EQ && value != null) {
            return new HashSet<>(Collections.singleton(String.valueOf(value)));
        }
        else if(op == Operator.GT || op == Operator.LT || op == Operator.GTE || op == Operator.LTE && value != null) {
            if(dimension.equals(Dimension.MONTH_YEAR)){
                return getValidSet(value, op, 2025, 2019);
            }
            else if(dimension.equals(Dimension.MONTH_QUARTER)){
                return getValidSet(value, op, 4, 1);
            }
            else if(dimension.equals(Dimension.MONTH)){
                return getValidSet(value, op, 12, 1);
            }
            else if(dimension.equals(Dimension.EVENT_SUBTYPE_SEVERITY)){
                return getValidSet(value, op, 5, 1);
            }
            else if(dimension.equals(Dimension.MONTH_CODE)){
                return getValidMonthCodes(value, op);
            }
        }
        else if (op == Operator.IN && value instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .collect(Collectors.toSet());
        }
        else if(op == Operator.NOT_IN && value instanceof List<?> list) {
            Set<String> exclude = list.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .collect(Collectors.toSet());
            if(dimension.equals(Dimension.ZONE_NAME)){
                return getNotIn(ExactMatchAccuracy.allZoneNames(), exclude);
            }
            else if(dimension.equals(Dimension.ZONE_DIVISION)){
                return getNotIn(ExactMatchAccuracy.allZoneDivisions(), exclude);
            }
            else if(dimension.equals(Dimension.EVENT_TYPE)){
                return getNotIn(ExactMatchAccuracy.allEventTypes(), exclude);
            }
            else if(dimension.equals(Dimension.EVENT_SUBTYPE)){
                return getNotIn(ExactMatchAccuracy.allEventSubTypes(), exclude);
            }
            else if(dimension.equals(Dimension.EVENT_SUBTYPE_SEVERITY)){
                return getNotIn(ExactMatchAccuracy.allSeverities(), exclude);
            }
            else if(dimension.equals(Dimension.MONTH)){
                return getNotIn(ExactMatchAccuracy.allMonths(), exclude);
            }
            else if(dimension.equals(Dimension.MONTH_CODE)){
                return getNotIn(ExactMatchAccuracy.allMonthCodes(), exclude);
            }
            else if(dimension.equals(Dimension.MONTH_YEAR)){
                return getNotIn(ExactMatchAccuracy.allYears(), exclude);
            }
            else if(dimension.equals(Dimension.MONTH_QUARTER)){
                return getNotIn(ExactMatchAccuracy.allQuarters(), exclude);
            }
        }
        return Collections.emptySet();
    }
    private Set<String> convertYear(Set<String> years) {
        return years.stream()
                .flatMap(year -> IntStream.rangeClosed(1, 12)
                        .mapToObj(month -> String.format("%s-%02d", year, month)))
                .collect(Collectors.toSet());
    }
    private Set<String> convertMonthAndYear(Set<String> years, Set<String> months) {
        Set<String> convertedMonthCodes = new HashSet<>();
        if(years == null || months == null || years.isEmpty() || months.isEmpty()) return convertedMonthCodes;
        for (String year : years) {
            for (String month : months) {
                try {
                    int m = Integer.parseInt(month);
                    convertedMonthCodes.add(String.format("%s-%02d", year, m));
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }
        return convertedMonthCodes;
    }
    private Set<String> convertQuarterAndYear(Set<String> years, Set<String> quarters) {
        Set<String> convertedMonthCodes = new HashSet<>();
        if(years == null || quarters == null || years.isEmpty() || quarters.isEmpty()) return convertedMonthCodes;
        for (String year : years) {
            for (String quarter : quarters) {
                int q;
                try {
                    q = Integer.parseInt(quarter);
                } catch (NumberFormatException e) { continue; }
                int startMonth = (q - 1) * 3 + 1;
                for (int i = 0; i < 3; i++) {
                    int month = startMonth + i;
                    convertedMonthCodes.add(String.format("%s-%02d", year, month));
                }
            }
        }
        return convertedMonthCodes;
    }
    private Set<String> convertZoneDivisionToZoneNames(Set<String> zoneDivisions) {
        Set<String> convertedZoneNames = new HashSet<>();
        if(zoneDivisions == null || zoneDivisions.isEmpty()) return convertedZoneNames;
        for (String zoneDivision : zoneDivisions) {
         for(String zoneName : zoneToDivisionMap.keySet()) {
             if(zoneToDivisionMap.get(zoneName).equals(zoneDivision)) {
                 convertedZoneNames.add(zoneName);
             }
         }
        }
        return convertedZoneNames;
    }
    private Set<String> convertEventTypeToSubType(Set<String> eventTypes) {
        Set<String> convertedSubTypes = new HashSet<>();
        if(eventTypes == null || eventTypes.isEmpty()) return convertedSubTypes;
        for (String eventType : eventTypes) {
            for(String subType : subtypeToTypeMap.keySet()) {
                if(subtypeToTypeMap.get(subType).equals(eventType)) {
                    convertedSubTypes.add(subType);
                }
            }
        }
        return convertedSubTypes;
    }
    private Set<String> convertSeverityToSubType(Set<String> severities) {
        Set<String> convertedSubTypes = new HashSet<>();
        if(severities == null || severities.isEmpty()) return convertedSubTypes;
        for (String severity : severities) {
            Long sev = Long.parseLong(severity);
            for(String subType : SEVERITY_MAP.keySet()) {
                if(SEVERITY_MAP.get(subType).equals(sev)) {
                    convertedSubTypes.add(subType);
                }
            }
        }
        return convertedSubTypes;
    }
    private Set<String> intersect(Set<String> existing, Set<String> incoming) {
        if (existing == null || existing.isEmpty()) return incoming;
        if(incoming == null || incoming.isEmpty()) return existing;
        existing.retainAll(incoming);
        return existing;
    }
    private Set<String> getValidSet(Object value, Operator op, Integer hi, Integer lo) {
        int target = Integer.parseInt(value.toString());
        Set<String> matched = new HashSet<>();
        for (int i = lo; i <= hi; i++) {
            boolean match = false;
            if (op == Operator.GT) match = i > target;
            else if (op == Operator.LT) match = i < target;
            else if (op == Operator.GTE) match = i >= target;
            else if (op == Operator.LTE) match = i <= target;
            if (match) matched.add(String.valueOf(i));
        }
        return matched;
    }
    private Set<String> getValidMonthCodes(Object value, Operator op) {
        YearMonth target = YearMonth.parse(value.toString());
        YearMonth start = YearMonth.parse("2019-01");
        YearMonth end = YearMonth.parse("2025-12");
        Set<String> matched = new HashSet<>();
        YearMonth current = start;
        while (!current.isAfter(end)) {
            boolean match = false;
            if (op == Operator.GT) match = current.isAfter(target);
            else if (op == Operator.LT) match = current.isBefore(target);
            else if (op == Operator.GTE) match = !current.isBefore(target);
            else if (op == Operator.LTE) match = !current.isAfter(target);
            if (match) matched.add(current.toString());
            current = current.plusMonths(1);
        }
        return matched;
    }
    private Set<String> getNotIn(Set<String> fullSet, Set<String> excludeSet) {
        Set<String> result = new HashSet<>(fullSet);
        result.removeAll(excludeSet);
        return result;
    }
    public static final Map<String, String> subtypeToTypeMap = Map.ofEntries(
            Map.entry("arms_act", "recovery"),
            Map.entry("explosive_act", "recovery"),
            Map.entry("narcotics", "recovery"),
            Map.entry("smuggling", "recovery"),
            Map.entry("murder", "crime"),
            Map.entry("dacoity", "crime"),
            Map.entry("robbery", "crime"),
            Map.entry("riot", "crime"),
            Map.entry("kidnapping", "crime"),
            Map.entry("police_assault", "crime"),
            Map.entry("women_and_child_repression", "crime"),
            Map.entry("theft", "crime"),
            Map.entry("burglary", "crime"),
            Map.entry("speedy_trial", "crime"),
            Map.entry("other_cases", "crime")
    );
    public static final Map<String, String> zoneToDivisionMap = Map.ofEntries(
            Map.entry("dmp", "Dhaka"),
            Map.entry("dhaka_range", "Dhaka"),
            Map.entry("gmp", "Dhaka"),
            Map.entry("cmp", "Chittagong"),
            Map.entry("chittagong_range", "Chittagong"),
            Map.entry("kmp", "Khulna"),
            Map.entry("khulna_range", "Khulna"),
            Map.entry("smp", "Sylhet"),
            Map.entry("sylhet_range", "Sylhet"),
            Map.entry("rmp", "Rajshahi"),
            Map.entry("rajshahi_range", "Rajshahi"),
            Map.entry("bmp", "Barishal"),
            Map.entry("barishal_range", "Barishal"),
            Map.entry("rpmp", "Rangpur"),
            Map.entry("rangpur_range", "Rangpur"),
            Map.entry("mymensingh_range", "Mymensingh"),
            Map.entry("ralway_range", "Railway")
    );
}
