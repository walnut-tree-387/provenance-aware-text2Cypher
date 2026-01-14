package com.example.text2cypher.data.cqp.factories;

import com.example.text2cypher.data.cqp.entities.CanonicalQueryPlan;
import com.example.text2cypher.data.dto.BaseQueryDto;

public interface CqpFactory<T extends BaseQueryDto> {
    CanonicalQueryPlan fromDto(T dto);
}
