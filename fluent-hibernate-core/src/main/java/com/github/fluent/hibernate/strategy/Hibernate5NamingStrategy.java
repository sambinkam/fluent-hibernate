package com.github.fluent.hibernate.strategy;

import java.lang.reflect.Field;
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

import com.github.fluent.hibernate.internal.util.InternalUtils;

/**
 * A naming strategy for Hibernate 5.
 *
 * TODO embedded column names
 *
 * @author V.Ladynev
 */
public class Hibernate5NamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {

    private static final long serialVersionUID = 3482010804082494311L;

    private final HibernateNamingStrategy strategy = new HibernateNamingStrategy();

    private final JoinTableNames joinTableNames = new JoinTableNames();

    public void setTablePrefix(final String tablePrefix) {
        strategy.setTablePrefix(tablePrefix);
    }

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

        if (isEmbeddedColumn(source)) {
            String propertyName = getPropertyName(attributePath.getParent());
            String embeddedPropertyName = getPropertyName(attributePath);
            return toIdentifier(
                    strategy.embeddedPropertyToColumnName(propertyName, embeddedPropertyName),
                    source);
        }

        String propertyName = getPropertyName(attributePath);
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
        String ownerEntityTable = NamingStrategyUtils.unqualify(source.getOwningEntityNaming()
                .getEntityName());
        String associatedEntityTable = NamingStrategyUtils.unqualify(source
                .getNonOwningEntityNaming().getEntityName());

        String tableName = strategy.collectionTableName(ownerEntityTable, associatedEntityTable);

        String result = joinTableNames.hasSameNameForOtherProperty(tableName, source) ? strategy
                .collectionTableName(ownerEntityTable, associatedEntityTable,
                        getPropertyName(source.getAssociationOwningAttributePath())) : tableName;

                joinTableNames.put(result, source);

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
