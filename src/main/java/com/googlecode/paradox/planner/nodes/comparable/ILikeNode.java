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
import com.googlecode.paradox.rowset.ValuesConverter;
import com.googlecode.paradox.utils.Expressions;

import java.sql.SQLException;
import java.util.List;

/**
 * Insensitive like node.
 *
 * @version 1.5
 * @since 1.6.0
 */
public class ILikeNode extends LikeNode {

    /**
     * Create a new instance.
     *
     * @param field    the first node.
     * @param last     the last node.
     * @param position the current Scanner position.
     */
    public ILikeNode(final FieldNode field, final FieldNode last,
                     final ScannerPosition position) {
        super(field, last, position);
        this.name = "ilike";
    }

    @Override
    public boolean evaluate(final ConnectionInfo connectionInfo, final Object[] row, final Object[] parameters,
                            final ParadoxType[] parameterTypes, final List<Column> columnsLoaded) throws SQLException {
        final Object value1 = FieldValueUtils.getValue(connectionInfo, row, field, parameters, parameterTypes,
                columnsLoaded);
        final Object value2 = FieldValueUtils.getValue(connectionInfo, row, last, parameters, parameterTypes,
                columnsLoaded);

        if (value1 == null || value2 == null) {
            return false;
        }

        return Expressions.accept(connectionInfo.getLocale(),
                ValuesConverter.getString(value1), ValuesConverter.getString(value2), false, escape);
    }
}
