package com.example.text2cypher.data.dto;

import lombok.Data;

@Data
public class TemporalAggregationRequestDto {
    private String queryTypeCode;  // a1
    private String eventType; // crime / recovery
    private String monthCode; // 2024-01
    private Long monthValue; // 01
    private Long monthQuarter; // 1 etc.
    private Long monthYear; // 2024 etc.
    private String zone; // dmp, dhaka_range etc.
    private String eventSubType; // dakoity, smuggling etc
}
