package com.example.text2cypher.ais_evaluation.utils;

import com.example.text2cypher.cypher_utils.cypher.OlapCypherResponse;
import com.example.text2cypher.cypher_utils.cypher.ProvenanceRecord;
import com.example.text2cypher.utils.LocalMapper;

import java.util.*;

public class ProvenanceChecker {

    public static boolean checkProvenance(
            OlapCypherResponse predicted,
            String goldProvenance,
            String goldResult
    ) {
        List<ProvenanceRecord> goldProvenanceList =
                LocalMapper.readList(goldProvenance, ProvenanceRecord.class);

        List<List<Map<String, Object>>> goldResults = LocalMapper.read(goldResult, List.class);
        if (!resultsMatch(goldResults, predicted.results())) {
            return false;
        }
        if (goldProvenanceList.size() != predicted.nodeList().size()) {
            return false;
        }
        return provenanceSetMatch(
                goldProvenanceList,
                predicted.nodeList()
        );
    }
    private static boolean resultsMatch(List<List<Map<String, Object>>> gold, List<List<Map<String, Object>>> predicted) {
        if (gold.size() != predicted.size()) return false;
        for (int i = 0; i < gold.size(); i++) {
            List<Map<String, Object>> goldRow = gold.get(i);
            List<Map<String, Object>> predRow = predicted.get(i);
            if (goldRow.size() != predRow.size()) return false;
            for (int j = 0; j < goldRow.size(); j++) {
                if (!Objects.equals(goldRow.get(j), predRow.get(j))) {
                    return false;
                }
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
        return Objects.equals(a, b);
    }
}

