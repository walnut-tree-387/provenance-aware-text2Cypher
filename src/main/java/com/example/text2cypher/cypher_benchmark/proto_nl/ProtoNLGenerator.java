package com.example.text2cypher.cypher_benchmark.proto_nl;
import com.example.text2cypher.cypher_utils.cqp.Filter;
import com.example.text2cypher.cypher_utils.cqp.GroupKey;
import com.example.text2cypher.cypher_utils.cqp.OrderSpec;
import com.example.text2cypher.cypher_utils.cqp.PostAggregationType;
import com.example.text2cypher.cypher_benchmark.dto.OlapQueryDto;
import com.example.text2cypher.cypher_benchmark.dto.PostAggregationDto;
import com.example.text2cypher.cypher_benchmark.dto.QueryType;
import org.springframework.stereotype.Component;

@Component
public class ProtoNLGenerator {
    public String generate(OlapQueryDto gqp) {
        StringBuilder protNl = new StringBuilder(getProtoKey(gqp));
        protNl.append(" filter by ");
        for(Filter constraint : gqp.getFilters()) {
            protNl.append(" ").append(constraint.getDimension()).append(" ")
                    .append(constraint.getValue()).append(",");
        }
        return protNl.toString();
    }

    private String getProtoKey(OlapQueryDto dto) {
        StringBuilder protoKey = new StringBuilder();
        if(dto.getQueryType().equals(QueryType.PRIORITY_ORDER)) {
            getPriorityOrderProtoKey(protoKey, dto);
        }
        else if(dto.getQueryType().equals(QueryType.COUNT)){
            getCountProtoKey(protoKey, dto);
        }
        else if(dto.getQueryType().equals(QueryType.AGGREGATION)){
            getAggregationProtoKey(protoKey, dto);
        }
        else if(dto.getQueryType().equals(QueryType.DOMINANT_ATTRIBUTION)){
            getDominantAttributionProtoKey(protoKey, dto);
        }
        else if(dto.getQueryType().equals(QueryType.DIFFERENCE)){
            getDifferenceProtoKey(protoKey, dto);
        }
        else if(dto.getQueryType().equals(QueryType.RATIO)){
            getRatioProtoKey(protoKey, dto);
        }
        else if(dto.getQueryType().equals(QueryType.BOOLEAN)){
            getBooleanProtoKey(protoKey, dto);
        }
        else if(dto.getQueryType().equals(QueryType.RANKING)){
            getRankingProtoKey(protoKey, dto);
        }
        return protoKey.toString();
    }
    public void getRankingProtoKey(StringBuilder protoKey, OlapQueryDto dto) {
        protoKey.append("Find a  details ranking for ");
        for(GroupKey g: dto.getGroupBy()){
            protoKey.append(g.getDimension()).append(", ");
        }
        protoKey.append(" after ordering them according to ");
        for(OrderSpec order: dto.getOrders()){
            protoKey.append(order.getField()).append(" ").append(order.getDirection()).append(", ");
        }
        protoKey.append("\n");
    }
    public void getBooleanProtoKey(StringBuilder protoKey, OlapQueryDto dto) {
        protoKey.append("Find a Boolean answer if ");
        for(int i = 0; i < dto.getPostAggregations().size(); i++) {
            PostAggregationDto postAggregation = dto.getPostAggregations().get(i);
            if(checkIfBooleanComparison(postAggregation.getType())) {
                protoKey.append(postAggregation.getArgs().getFirst()).append(" ")
                        .append(postAggregation.getType().value).append(" ").append(postAggregation.getArgs().getLast());
            }
        }
        protoKey.append("\n");
    }
    public void getRatioProtoKey(StringBuilder protoKey,OlapQueryDto dto) {
        protoKey.append("Calculate the ratio between ");
        for(int i = 0; i < dto.getPostAggregations().size(); i++) {
            PostAggregationDto postAggregation = dto.getPostAggregations().get(i);
            if(postAggregation.getType().equals(PostAggregationType.RATIO)) {
                protoKey.append(postAggregation.getArgs().getFirst()).append(" AND ").append(postAggregation.getArgs().getLast());
            }
        }
        protoKey.append("\n");
    }
    public void getDifferenceProtoKey(StringBuilder protoKey,OlapQueryDto dto) {
        protoKey.append("Calculate the difference between ");
        for(int i = 0; i < dto.getPostAggregations().size(); i++) {
            PostAggregationDto postAggregation = dto.getPostAggregations().get(i);
            if(postAggregation.getType().equals(PostAggregationType.DIFFERENCE)) {
                protoKey.append(postAggregation.getArgs().getFirst()).append(" AND ").append(postAggregation.getArgs().getLast());
            }
        }
        protoKey.append("\n");
    }
    public void getCountProtoKey(StringBuilder protNl, OlapQueryDto dto) {
        protNl.append("Find the count ");
        for(String r : dto.getReturns()) protNl.append(r).append(", ");
    }
    public void getAggregationProtoKey(StringBuilder protNl, OlapQueryDto dto) {
        protNl.append("Find the total sum of ");
        for(String r : dto.getReturns()) protNl.append(r).append(", ");
    }
    public void getDominantAttributionProtoKey(StringBuilder protNl, OlapQueryDto dto) {
        protNl.append("Find the dominant ");
        for(GroupKey g : dto.getGroupBy()) protNl.append(g.getDimension()).append(", ");
        protNl.append(" according to the order of ");
        for(OrderSpec order: dto.getOrders()) protNl.append(order.getField()).append(", ");
    }
    public void getPriorityOrderProtoKey(StringBuilder protoKey, OlapQueryDto dto) {
        protoKey.append("Rank ");
        for(GroupKey groupKey : dto.getGroupBy()) {
            protoKey.append(groupKey.getDimension().toString().toLowerCase()).append(",");
        }
        protoKey.append(" according to ");
        for(OrderSpec order: dto.getOrders()) {
            protoKey.append(order.getField()).append(" ").append(order.getDirection()).append(",");
        }
        if(dto.getLimit() != null)protoKey.append(" fetch top ").append(dto.getLimit());
        if(dto.getOffset() != null)protoKey.append(" skip first ").append(dto.getLimit()).append(" row ");
    }
    public Boolean checkIfBooleanComparison(PostAggregationType type){
        return type.equals(PostAggregationType.EQ) || type.equals(PostAggregationType.GT) || type.equals(PostAggregationType.LT) ||
                type.equals(PostAggregationType.GTE) || type.equals(PostAggregationType.LTE);
    }
}
