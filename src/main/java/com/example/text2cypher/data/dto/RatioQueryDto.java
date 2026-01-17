package com.example.text2cypher.data.dto;

import com.example.text2cypher.data.cqp.entities.Constraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@EqualsAndHashCode(callSuper = true)
@Data
public class RatioQueryDto extends BaseQueryDto{
    private Constraint constraint; // numerator constraint
}
