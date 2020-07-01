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
package com.googlecode.paradox.parser.nodes.comparable;

import com.googlecode.paradox.ParadoxConnection;
import com.googlecode.paradox.parser.ValuesComparator;
import com.googlecode.paradox.parser.nodes.FieldNode;

/**
 * Stores the greater than node.
 *
 * @author Leonardo Costa
 * @version 1.5
 * @since 1.1
 */
public final class GreaterThanNode extends AbstractComparableNode {

    /**
     * Create a new instance.
     *
     * @param connection the Paradox connection.
     * @param field      the first node.
     * @param last       the last node.
     */
    public GreaterThanNode(final ParadoxConnection connection, final FieldNode field, final FieldNode last) {
        super(connection, ">", field, last);
    }

    @Override
    public boolean evaluate(final Object[] row, final ValuesComparator comparator) {
        final Object value1 = getValue(row, field);
        final Object value2 = getValue(row, last);
        return comparator.compare(value1, value2, i -> i == 1);
    }
}
