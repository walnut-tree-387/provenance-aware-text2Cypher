package com.example.text2cypher.data.cqp;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
            case Set<?> set -> {
                StringBuilder result = new StringBuilder("[");
                Iterator<?> it = set.iterator();
                while (it.hasNext()) {
                    Object item = it.next();

                    if (item instanceof String) {
                        result.append("'").append(item).append("'");
                    } else if (item instanceof Number) {
                        result.append(item);
                    } else {
                        throw new IllegalStateException(
                                "Unsupported set item type: " + item.getClass()
                        );
                    }
                    if (it.hasNext()) {
                        result.append(", ");
                    }
                }
                result.append("]");
                return result.toString();
            }
            default -> throw new IllegalStateException("Unsupported value type: " + this.value.getClass());
        }
    }
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof Filter other)) return false;
//
//        if (dimension != other.dimension) return false;
//        if (operator != other.operator) return false;
//
//        return valuesEqual(value, other.value);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(
//                dimension,
//                operator,
//                normalizedValue(value)
//        );
//    }
//
//    private boolean valuesEqual(Object v1, Object v2) {
//        if (v2 instanceof List<?> l2) {
//            return (v1).equals(new HashSet<>(l2));
//        }
//        return Objects.equals(v1, v2);
//    }
//
//    private Object normalizedValue(Object v) {
//        if (v instanceof List<?> list) {
//            return new HashSet<>(list);
//        }
//        return v;
//    }
}
