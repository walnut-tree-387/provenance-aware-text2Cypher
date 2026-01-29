package com.example.text2cypher.data.cqp.entities.OLAP_entities;

import com.example.text2cypher.data.cqp.entities.Operator;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Filter {
    private Dimension dimension;
    private Operator operator;
    private Object value;
    public String valueToCypher() {
        switch (this.value) {
            case String ignored -> {
                return "'" + this.value + "'";
            }
            case Number ignored -> {
                return this.value.toString();
            }
            case List<?> list -> {
                StringBuilder result = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof String) {
                        result.append("'").append(item).append("'");
                    } else if (item instanceof Number) {
                        result.append(item);
                    } else {
                        throw new IllegalStateException("Unsupported list item type: " + item.getClass());
                    }
                    if (i < list.size() - 1) {
                        result.append(", ");
                    }
                }
                result.append("]");
                return result.toString();
            }
            default -> throw new IllegalStateException("Unsupported value type: " + this.value.getClass());
        }
    }

}
