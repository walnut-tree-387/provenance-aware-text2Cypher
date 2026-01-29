package com.example.text2cypher.data.cqp.factories;

import com.example.text2cypher.data.cqp.entities.OLAP_entities.*;
import com.example.text2cypher.data.dto.OlapQueryDto;
import com.example.text2cypher.data.dto.PostAggregationDto;
import org.springframework.stereotype.Component;

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
                compilePostAggregations(dto),
                compileOrder(dto),
                compileProjection(dto)
        );
    }
    private List<Filter> compileFilters(OlapQueryDto dto) {
        return (dto.getFilters() == null) ? List.of() : dto.getFilters();
    }
    private List<Dimension> compileGroupBy(OlapQueryDto dto) {
        return dto.getGroupBy() == null ? List.of() : dto.getGroupBy();
    }
    private List<Measure> compileMeasures(OlapQueryDto dto) {
        return dto.getMeasures().stream()
                .map(m ->
                        new Measure(
                                m.getAggregationType(),
                                m.getAggregationSemantic(),
                                Fact.OBSERVATION_COUNT,
                                m.getAlias(),
                                m.getFilters()
                        )
                )
                .toList();
    }
    private OrderSpec compileOrder(OlapQueryDto dto) {
        if (dto.getOrder() == null) return null;
        return new OrderSpec(
                dto.getOrder().getField(),
                dto.getOrder().getDirection(),
                dto.getOrder().getLimit()
        );
    }

    private Projection compileProjection(OlapQueryDto dto) {
        return new Projection(dto.getProjection());
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
            case SHARE -> new Share(
                    spec.getArgs().get("part"),
                    spec.getArgs().get("total"),
                    spec.getAlias()
            );
        };
    }

}

