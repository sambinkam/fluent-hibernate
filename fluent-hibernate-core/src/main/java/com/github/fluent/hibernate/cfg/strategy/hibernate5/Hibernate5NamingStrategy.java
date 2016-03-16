package com.github.fluent.hibernate.cfg.strategy.hibernate5;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitForeignKeyNameSource;
import org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitJoinTableNameSource;
import org.hibernate.boot.model.naming.ImplicitNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.cfg.Ejb3Column;
import org.hibernate.cfg.PropertyHolder;
import org.hibernate.internal.util.ReflectHelper;

import com.github.fluent.hibernate.annotations.FluentName;
import com.github.fluent.hibernate.cfg.strategy.HibernateNamingStrategy;
import com.github.fluent.hibernate.cfg.strategy.JoinTableNames;
import com.github.fluent.hibernate.cfg.strategy.JoinTableNames.TableDescription;
import com.github.fluent.hibernate.cfg.strategy.NamingStrategyUtils;
import com.github.fluent.hibernate.internal.util.InternalUtils;

/**
 * A naming strategy for Hibernate 5.
 *
 * @author V.Ladynev
 */
public class Hibernate5NamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {

    private static final long serialVersionUID = 3482010804082494311L;

    private final HibernateNamingStrategy strategy = new HibernateNamingStrategy();

    private final JoinTableNames joinTableNames = new JoinTableNames();

    public void setTablePrefix(String tablePrefix) {
        strategy.setTablePrefix(tablePrefix);
    }

    /**
     * Get a name of a table for persistent.
     */
    @Override
    protected String transformEntityName(EntityNaming entityNaming) {
        return strategy.classToTableName(entityNaming.getEntityName());
    }

    @Override
    public Identifier determineBasicColumnName(ImplicitBasicColumnNameSource source) {
        // don't know what it means
        if (source.isCollectionElement()) {
            return toIdentifier("elt", source);
        }

        AttributePath attributePath = source.getAttributePath();
        String propertyName = getPropertyName(attributePath);

        Method propertyGetter = getPropertyGetter(source, propertyName);

        if (isEmbeddedColumn(source)) {

            String embeddedPropertyName = getPropertyName(attributePath);

            String embeddedPrefix = getEmbeddedPrefix(source);

            boolean hasEmbeddedPrefix = !InternalUtils.StringUtils.isEmpty(embeddedPrefix);

            String prefix = hasEmbeddedPrefix ? embeddedPrefix : getPropertyName(attributePath
                    .getParent());

            return toIdentifier(strategy.embeddedPropertyToColumnName(prefix, embeddedPropertyName,
                    hasEmbeddedPrefix), source);
        }

        if (propertyName.equals("firstPartnerLocation")) {

        }

        System.out.println(propertyName);

        // Hibernate calls this for @Embedded column, but doesn't use
        return toIdentifier(strategy.propertyToColumnName(propertyName), source);
    }

    private boolean isEmbeddedColumn(ImplicitBasicColumnNameSource source) {
        if (InternalUtils.StringUtils
                .isEmpty(getPropertyName(source.getAttributePath().getParent()))) {
            return false;
        }

        Ejb3Column column = getEjb3Column(source);
        PropertyHolder propertyHolder = column.getPropertyHolder();
        return propertyHolder.isComponent();
    }

    // TODO check a field annotation
    private String getEmbeddedPrefix(ImplicitBasicColumnNameSource source) {
        Ejb3Column column = getEjb3Column(source);

        String propertyName = getPropertyName(source.getAttributePath().getParent());
        try {

            Class<?> mappedClass = column.getPropertyHolder().getPersistentClass().getMappedClass();

            Method getter = ReflectHelper.findGetterMethod(mappedClass, propertyName);

            FluentName annotation = getter.getAnnotation(FluentName.class);

            if (annotation != null) {
                return annotation.prefix();
            } else {
                return null;
            }

        } catch (Exception ex) {
            return null;
        }

    }

    private Method getPropertyGetter(ImplicitBasicColumnNameSource source, String propertyName) {
        Ejb3Column column = getEjb3Column(source);
        Class<?> mappedClass = column.getPropertyHolder().getPersistentClass().getMappedClass();
        return ReflectHelper.findGetterMethod(mappedClass, propertyName);
    }

    private Ejb3Column getEjb3Column(ImplicitBasicColumnNameSource source) {
        try {
            Field ejb3ColumnField = source.getClass().getDeclaredField("this$0");
            ejb3ColumnField.setAccessible(true);
            return (Ejb3Column) ejb3ColumnField.get(source);
        } catch (Exception ex) {
            throw InternalUtils.toRuntimeException(ex);
        }
    }

    @Override
    public Identifier determineJoinColumnName(ImplicitJoinColumnNameSource source) {
        String propertyTableName = NamingStrategyUtils.unqualify(source.getEntityNaming()
                .getEntityName());
        // a property name is null for join tables for an owner table foreign key
        String propertyName = getPropertyName(source.getAttributePath());
        String result = strategy.foreignKeyColumnName(propertyName, propertyTableName);
        return toIdentifier(result, source);
    }

    @Override
    public Identifier determineJoinTableName(ImplicitJoinTableNameSource source) {
        String ownerEntityTable = source.getOwningEntityNaming().getEntityName();
        String associatedEntityTable = source.getNonOwningEntityNaming().getEntityName();

        String propertyName = getPropertyName(source.getAssociationOwningAttributePath());

        String tableName = strategy.joinTableName(ownerEntityTable, associatedEntityTable);

        TableDescription description = new TableDescription(ownerEntityTable,
                associatedEntityTable, propertyName);

        String result = joinTableNames.hasSameNameForOtherProperty(tableName, description) ? strategy
                .joinTableName(ownerEntityTable, associatedEntityTable, propertyName) : tableName;

        joinTableNames.put(result, description);

        return toIdentifier(result, source);
    }

    @Override
    public Identifier determineForeignKeyName(ImplicitForeignKeyNameSource source) {
        List<Identifier> columnNames = source.getColumnNames();

        // constraints are supported for one column only
        if (InternalUtils.CollectionUtils.size(columnNames) != 1) {
            return super.determineForeignKeyName(source);
        }

        String result = strategy.foreignKeyName(source.getTableName().getText(), columnNames.get(0)
                .getText());

        return toIdentifier(result, source);
    }

    @Override
    public Identifier determineUniqueKeyName(ImplicitUniqueKeyNameSource source) {
        List<Identifier> columnNames = source.getColumnNames();

        // constraints are supported for one column only
        if (InternalUtils.CollectionUtils.size(columnNames) != 1) {
            return super.determineUniqueKeyName(source);
        }

        String result = strategy.uniqueKeyName(source.getTableName().getText(), columnNames.get(0)
                .getText());

        return toIdentifier(result, source);
    }

    private static String getPropertyName(AttributePath attributePath) {
        return attributePath == null ? null : attributePath.getProperty();
    }

    private static Identifier toIdentifier(String stringForm, ImplicitNameSource source) {
        return source.getBuildingContext().getMetadataCollector().getDatabase()
                .getJdbcEnvironment().getIdentifierHelper().toIdentifier(stringForm);
    }

}
