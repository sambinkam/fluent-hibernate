package com.github.fluent.hibernate.cfg.strategy.hibernate5;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;

import com.github.fluent.hibernate.internal.util.InternalUtils;

/**
 *
 * @author V.Ladynev
 */
public final class StrategyTestUtils {

    private StrategyTestUtils() {

    }

    public static List<String> getComponentColumnNames(PersistentClass persistentClass,
            String propertyName) {
        ArrayList<String> result = InternalUtils.CollectionUtils.newArrayList();

        Property componentBinding = persistentClass.getProperty(propertyName);
        assertThat(componentBinding).isNotNull();

        Component component = (Component) componentBinding.getValue();
        Iterator<Selectable> selectables = component.getColumnIterator();

        while (selectables.hasNext()) {
            Column column = (Column) selectables.next();
            result.add(column.getQuotedName());
        }

        return result;
    }

}
