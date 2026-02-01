package com.example.text2cypher.data.cqp.factories;

import com.example.text2cypher.data.cqp.entities.OLAP_entities.*;
import com.example.text2cypher.data.dto.OlapQueryDto;
import com.example.text2cypher.data.dto.PostAggregationDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.example.text2cypher.data.dto.PostAggregationType.*;

@Component
public class OlapCqpFactory {

    public CQP fromDto(OlapQueryDto dto) {

        return new CQP(
                Fact.OBSERVATION_COUNT,
                compileFilters(dto),
                compileGroupBy(dto),
                compileMeasures(dto),
                compileProvenanceFilters(),
                compilePostAggregations(dto),
                compileOrder(dto),
                dto.getLimit(),
                dto.getOffset(),
                compileProjection(dto)
        );
    }
    private List<Filter> compileProvenanceFilters(){
        return new ArrayList<>();
    }
    private List<Filter> compileFilters(OlapQueryDto dto) {
        return (dto.getFilters() == null) ? List.of() : dto.getFilters();
    }
    private List<GroupKey> compileGroupBy(OlapQueryDto dto) {
        return dto.getGroupBy() == null ? List.of() : dto.getGroupBy();
    }
    private List<Measure> compileMeasures(OlapQueryDto dto) {
        return dto.getMeasures().stream()
                .map(m ->
                        new Measure(
                                m.getAggregationType(),
                                m.getAlias(),
                                m.getFilters()
                        )
                )
                .toList();
    }
    private List<OrderSpec> compileOrder(OlapQueryDto dto) {
        return dto.getOrders().stream()
                .map(o -> new OrderSpec(o.getField(), o.getDirection(), o.getOrderType()))
                .toList();
    }


    private List<String> compileProjection(OlapQueryDto dto) {
        return dto.getProjection();
    }

    private List<PostAggregation> compilePostAggregations(OlapQueryDto dto) {
        if (dto.getPostAggregations() == null) return List.of();

        return dto.getPostAggregations().stream()
                        .map(this::compilePostAggregation)
                        .toList();

    }
    private PostAggregation compilePostAggregation(PostAggregationDto spec) {
        return switch (spec.getType()) {
            case RATIO -> new Ratio(
                    spec.getArgs().get("numerator"),
                    spec.getArgs().get("denominator"),
                    spec.getAlias()
            );
            case DIFFERENCE -> new Difference(
                    spec.getArgs().get("left"),
                    spec.getArgs().get("right"),
                    spec.getAlias()
            );
            default -> new Comparison(
                    spec.getAlias(),
                    spec.getType(),
                    spec.getArgs().get("part"),
                    spec.getArgs().get("total")
            );
        };
    }

}

