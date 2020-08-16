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
package com.googlecode.paradox.data;

import com.googlecode.paradox.data.field.*;
import com.googlecode.paradox.exceptions.DataError;
import com.googlecode.paradox.exceptions.ParadoxDataException;
import com.googlecode.paradox.metadata.Field;
import com.googlecode.paradox.metadata.paradox.ParadoxTable;

import java.nio.ByteBuffer;
import java.sql.SQLException;

/**
 * Factory for Paradox field parsers.
 *
 * @version 1.3
 * @since 1.3
 */
public final class ParadoxFieldFactory {

    /**
     * Stores all available Paradox field parsers.
     */
    private static final FieldParser[] ALL_PARSES = {
            new AutoIncrementField(),
            new BooleanField(),
            new BlobField(),
            new DateField(),
            new IntegerField(),
            new LongField(),
            new MemoField(),
            new NumberField(),
            new TimeField(),
            new TimestampField(),
            new VarcharField(),
            new BCDField(),
            new BytesField()
    };

    /**
     * Utility class.
     */
    private ParadoxFieldFactory() {
        // Utility class.
    }

    /**
     * Parses the filter;
     *
     * @param table  the  table.
     * @param buffer the buffer to read of.
     * @param field  the  field.
     * @return the parsed value.
     * @throws SQLException in case of parse errors.
     */
    public static Object parse(final ParadoxTable table, final ByteBuffer buffer, final Field field)
            throws SQLException {
        for (final FieldParser parser : ALL_PARSES) {
            if (parser.match(field.getType())) {
                return parser.parse(table, buffer, field);
            }
        }

        throw new ParadoxDataException(DataError.FIELD_TYPE_NOT_SUPPORTED);
    }
}
