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
package com.googlecode.paradox.data.field;

import com.googlecode.paradox.data.FieldParser;
import com.googlecode.paradox.data.table.value.FieldValue;
import com.googlecode.paradox.metadata.ParadoxField;
import com.googlecode.paradox.metadata.ParadoxTable;
import com.googlecode.paradox.results.ParadoxFieldType;

import java.nio.ByteBuffer;
import java.sql.Types;
import java.util.Arrays;

/**
 * Parses a VARCHAR field.
 *
 * @author Leonardo Alves da Costa
 * @version 1.0
 * @since 1.3
 */
public final class VarcharField implements FieldParser {

    private static final FieldValue NULL = new FieldValue(Types.VARCHAR);

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean match(final int type) {
        return type == ParadoxFieldType.VARCHAR.getType();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public FieldValue parse(final ParadoxTable table, final ByteBuffer buffer, final ParadoxField field) {
        final ByteBuffer valueString = ByteBuffer.allocate(field.getSize());

        // reset buffer to zeros
        Arrays.fill(valueString.array(), (byte) 0);

        for (int chars = 0; chars < field.getSize(); chars++) {
            valueString.put(buffer.get());
        }

        // FIXME review this code
        final String value = new String(valueString.array(), table.getCharset());
        if (value.isEmpty()) {
            return NULL;
        }

        return new FieldValue(value, Types.VARCHAR);
    }

}
