package com.example.text2cypher.data.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RankingQueryDto extends BaseQueryDto{
    private String rankDimension;
    private Long limit;
}
