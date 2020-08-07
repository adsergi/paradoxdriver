/*
 * Copyright (C) 2009 Leonardo Alves da Costa
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
 * License for more details. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.googlecode.paradox.planner.nodes;

import com.googlecode.paradox.ConnectionInfo;
import com.googlecode.paradox.data.TableData;
import com.googlecode.paradox.exceptions.ParadoxDataException;
import com.googlecode.paradox.metadata.ParadoxField;
import com.googlecode.paradox.metadata.ParadoxTable;
import com.googlecode.paradox.parser.nodes.*;
import com.googlecode.paradox.planner.collections.FixedValueCollection;
import com.googlecode.paradox.results.Column;

import java.sql.SQLException;
import java.util.*;

/**
 * Stores the execution plan table node.
 *
 * @version 1.4
 * @since 1.1
 */
public final class PlanTableNode {

    /**
     * The plan alias.
     */
    private String alias;

    /**
     * The plan table.
     */
    private ParadoxTable table;

    /**
     * The table join type.
     */
    private final JoinType joinType;
    /**
     * The table join filters.
     */
    private AbstractConditionalNode conditionalJoin;
    /**
     * Columns to load.
     */
    private final Set<Column> columns = new HashSet<>();

    /**
     * Creates a new instance.
     *
     * @param connectionInfo the connection information.
     * @param table          the table data to use.
     * @throws SQLException in case of failures.
     */
    public PlanTableNode(final ConnectionInfo connectionInfo, final TableNode table)
            throws SQLException {
        String schemaName = table.getSchemaName();
        if (schemaName == null) {
            schemaName = connectionInfo.getSchema();
        }

        final String tableName = table.getName();
        for (final ParadoxTable paradoxTable : TableData.listTables(schemaName, connectionInfo)) {
            if (schemaName.equalsIgnoreCase(paradoxTable.getSchemaName())
                    && tableName.equalsIgnoreCase(paradoxTable.getName())) {
                this.table = paradoxTable;
                break;
            }
        }

        if (this.table == null) {
            throw new ParadoxDataException(ParadoxDataException.Error.TABLE_NOT_FOUND, table.getPosition(), tableName);
        }

        this.alias = table.getAlias();

        if (table instanceof JoinNode) {
            final JoinNode join = (JoinNode) table;
            conditionalJoin = join.getCondition();
            joinType = join.getJoinType();
        } else {
            conditionalJoin = null;
            joinType = JoinType.INNER;
        }
    }

    /**
     * Gets the associated table field.
     *
     * @param field the table field.
     * @return the table field or <code>null</code> if not found.
     */
    public ParadoxField findField(final SQLNode field) {
        return Arrays.stream(table.getFields())
                .filter(f -> f.getName().equalsIgnoreCase(field.getName()))
                .findFirst().orElse(null);
    }

    /**
     * Adds a column list to be loaded in this table.
     *
     * @param columns the columns to add.
     */
    public void addColumns(final Collection<Column> columns) {
        columns.stream().filter(c -> c.isThis(this.table)).forEach(this.columns::add);
    }

    /**
     * Loads the table data.
     *
     * @return the table data.
     * @throws SQLException in case of failures.
     */
    public Collection<Object[]> load() throws SQLException {
        if (this.columns.isEmpty()) {
            return new FixedValueCollection<>(this.table.getRowCount(), new Object[0]);
            //return Arrays.asList(new Object[this.table.getRowCount()][0]);
        }

        return TableData.loadData(table, this.columns.stream().map(Column::getField).toArray(ParadoxField[]::new));
    }

    /**
     * Gets the columns to load.
     *
     * @return the columns to load.
     */
    public Set<Column> getColumns() {
        return columns;
    }

    /**
     * Gets the plan alias.
     *
     * @return the plan alias.
     */
    public String getAlias() {
        return this.alias;
    }

    /**
     * Sets the plan alias.
     *
     * @param alias the plan alias to set.
     */
    public void setAlias(final String alias) {
        this.alias = alias;
    }

    /**
     * Gets the table plan.
     *
     * @return the table plan.
     */
    public ParadoxTable getTable() {
        return this.table;
    }

    /**
     * Return if the name is referencing this table.
     *
     * @param aliasOrName the alias or table name.
     * @return <code>true</code> if is this table.
     */
    public boolean isThis(final String aliasOrName) {
        if (aliasOrName == null) {
            return true;
        }

        return aliasOrName.equalsIgnoreCase(table.getName()) || aliasOrName.equalsIgnoreCase(alias);
    }

    @Override
    public String toString() {
        if (alias != null) {
            return table.getName() + " as " + alias;
        }
        return table.getName();
    }

    /**
     * Gets the conditional join.
     *
     * @return the conditional join.
     */
    public AbstractConditionalNode getConditionalJoin() {
        return conditionalJoin;
    }

    /**
     * Sets the table conditional join.
     *
     * @param conditionalJoin the table conditional join
     */
    public void setConditionalJoin(AbstractConditionalNode conditionalJoin) {
        this.conditionalJoin = conditionalJoin;
    }

    /**
     * Gets the join type.
     *
     * @return the join type.
     */
    public JoinType getJoinType() {
        return joinType;
    }
}
