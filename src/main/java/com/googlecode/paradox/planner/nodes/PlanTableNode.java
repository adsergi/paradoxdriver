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
import com.googlecode.paradox.metadata.ParadoxTable;
import com.googlecode.paradox.parser.nodes.AbstractConditionalNode;
import com.googlecode.paradox.parser.nodes.JoinNode;
import com.googlecode.paradox.parser.nodes.JoinType;
import com.googlecode.paradox.parser.nodes.TableNode;

import java.sql.SQLException;

/**
 * Stores the execution plan table node.
 *
 * @version 1.3
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

    private AbstractConditionalNode conditionalJoin;

    private JoinType joinType = JoinType.INNER;

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
