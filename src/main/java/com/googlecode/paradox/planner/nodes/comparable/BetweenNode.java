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
package com.googlecode.paradox.planner.nodes.comparable;

import com.googlecode.paradox.ConnectionInfo;
import com.googlecode.paradox.parser.ScannerPosition;
import com.googlecode.paradox.planner.FieldValueUtils;
import com.googlecode.paradox.planner.nodes.FieldNode;
import com.googlecode.paradox.results.Column;
import com.googlecode.paradox.results.ParadoxType;
import com.googlecode.paradox.rowset.ValuesComparator;

import java.sql.SQLException;
import java.util.List;

/**
 * Stores the between node.
 *
 * @version 1.9
 * @since 1.1
 */
public final class BetweenNode extends AbstractComparableNode {

    /**
     * The field node.
     */
    private final FieldNode first;

    /**
     * Create a new instance.
     *
     * @param field    the middle node.
     * @param first    the first node.
     * @param last     the last node.
     * @param position the current Scanner position.
     */
    public BetweenNode(final FieldNode field, final FieldNode first,
                       final FieldNode last, final ScannerPosition position) {
        super("BETWEEN", field, last, position);
        this.first = first;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public String toString() {
        return this.field + " BETWEEN " + first + " AND " + last;
    }

    @Override
    public boolean evaluate(final ConnectionInfo connectionInfo, final Object[] row, final Object[] parameters,
                            final ParadoxType[] parameterTypes, final List<Column> columnsLoaded) throws SQLException {
        final Object value1 = FieldValueUtils.getValue(connectionInfo, row, field, parameters, parameterTypes,
                columnsLoaded);
        final Object value2 = FieldValueUtils.getValue(connectionInfo, row, first, parameters, parameterTypes,
                columnsLoaded);
        final Object value3 = FieldValueUtils.getValue(connectionInfo, row, last, parameters, parameterTypes,
                columnsLoaded);

        return ValuesComparator.compare(value1, value2, i -> i >= 0) &&
                ValuesComparator.compare(value1, value3, i -> i <= 0);
    }
}
