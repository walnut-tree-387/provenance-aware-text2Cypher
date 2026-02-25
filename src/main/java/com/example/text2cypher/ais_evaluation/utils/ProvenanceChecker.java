package com.example.text2cypher.ais_evaluation.utils;

import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponse;
import com.example.text2cypher.cypher_utils.cypher.ProvenanceRecord;
import com.example.text2cypher.utils.LocalMapper;

import java.util.*;

public class ProvenanceChecker {

    public static boolean checkProvenance(OlapCypherResponse predicted, String goldProvenance) {
        List<ProvenanceRecord> goldProvenanceList = LocalMapper.readList(goldProvenance, ProvenanceRecord.class);
        if (goldProvenanceList.size() != predicted.nodeList().size()) return false;
        return provenanceSetMatch(goldProvenanceList, predicted.nodeList());
    }
    public static boolean checkResult(OlapCypherResponse predicted, String goldResult){
        List<List<Map<String, Object>>> goldResults = LocalMapper.read(goldResult, List.class);
        return resultsMatch(goldResults, predicted.results());
    }
    private static boolean resultsMatch(List<List<Map<String, Object>>> gold, List<List<Map<String, Object>>> predicted) {
        if (gold.size() != predicted.size()) return false;
        for (int i = 0; i < gold.size(); i++) {
            List<Map<String, Object>> goldLayer = gold.get(i);
            List<Map<String, Object>> predLayer = predicted.get(i);
            if (goldLayer.size() != predLayer.size()) return false;
            List<Map<String, Object>> predRemaining = new ArrayList<>(predLayer);
            for (Map<String, Object> goldMap : goldLayer) {
                boolean foundMatch = false;
                for (int j = 0; j < predRemaining.size(); j++) {
                    if (valuesMatchRegardlessOfKeys(goldMap, predRemaining.get(j))) {
                        predRemaining.remove(j);
                        foundMatch = true;
                        break;
                    }
                }
                if (!foundMatch) return false;
            }
        }
        return true;
    }

    private static boolean provenanceSetMatch(List<ProvenanceRecord> gold, List<ProvenanceRecord> predicted) {
        List<ProvenanceRecord> remaining = new ArrayList<>(predicted);
        for (ProvenanceRecord g : gold) {
            Optional<ProvenanceRecord> match =
                    remaining.stream()
                            .filter(p -> provenanceExactlyMatch(g, p))
                            .findFirst();
            if (match.isEmpty()) {
                return false;
            }
            remaining.remove(match.get());
        }
        return true;
    }
    private static boolean provenanceExactlyMatch(ProvenanceRecord a, ProvenanceRecord b) {
        return a.id().equals(b.id());
    }
    private static boolean valuesMatchRegardlessOfKeys(Map<String, Object> a, Map<String, Object> b) {
        if (a.size() != b.size()) return false;
        List<Object> valuesA = new ArrayList<>(a.values());
        List<Object> valuesB = new ArrayList<>(b.values());
        for (Object valA : valuesA) {
            boolean found = false;
            for (int i = 0; i < valuesB.size(); i++) {
                if (areValuesEqual(valA, valuesB.get(i))) {
                    valuesB.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }
    private static boolean areValuesEqual(Object valA, Object valB) {
        if (valA == valB) return true;
        if (valA == null || valB == null) return false;
        if (valA instanceof Number && valB instanceof Number) {
            return Double.compare(((Number) valA).doubleValue(), ((Number) valB).doubleValue()) == 0;
        }
        return valA.equals(valB);
    }


}

