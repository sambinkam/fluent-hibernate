package com.github.fluent.hibernate.request.builder;

import java.util.Collection;

/**
 * @author alexey.pchelnikov.
 */
public final class Builders {

    private Builders() {

    }

    public static InBuilder in(String propertyName, Collection<?> values) {
        return new InBuilder(propertyName, values);
    }

    public static EqBuilder eq(String propertyName, Object value) {
        return new EqBuilder(propertyName, value);
    }

}
